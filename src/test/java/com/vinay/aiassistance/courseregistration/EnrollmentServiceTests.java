package com.vinay.aiassistance.courseregistration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinay.aiassistance.courseregistration.exception.ApiException;
import com.vinay.aiassistance.courseregistration.model.CourseOffering;
import com.vinay.aiassistance.courseregistration.repository.InMemoryEnrollmentRepository;
import com.vinay.aiassistance.courseregistration.repository.MockLearnerRepository;
import com.vinay.aiassistance.courseregistration.service.EnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class EnrollmentServiceTests {
    private MockLearnerRepository learners;
    private EnrollmentService service;

    @BeforeEach void setUp() {
        learners = new MockLearnerRepository(new ObjectMapper());
        service = new EnrollmentService(new InMemoryEnrollmentRepository(new CourseOffering("X", "JAVA-FUNDAMENTALS", 1)), learners);
    }

    @Test void enrollsEligibleLearnerWithSeat() {
        assertEquals("ENROLLED", service.enroll("X", "L101").outcome());
        assertEquals(List.of("L101"), List.copyOf(service.inspect("X").enrolledLearnerIds()));
    }

    @Test void queuesEligibleLearnerWhenFullAndDuplicateRequestsDoNotChangeState() {
        service.enroll("X", "L101");
        assertEquals("WAITLISTED", service.enroll("X", "L104").outcome());
        assertEquals("DUPLICATE", service.enroll("X", "L104").outcome());
        assertEquals(List.of("L104"), service.inspect("X").waitlist());
        assertEquals("DUPLICATE", service.enroll("X", "L101").outcome());
    }

    @Test void rejectsIneligibleLearnersWithoutStateChange() {
        assertEquals("REJECTED_PREREQUISITE", service.enroll("X", "L102").outcome());
        assertEquals("REJECTED_INACTIVE", service.enroll("X", "L103").outcome());
        assertTrue(service.inspect("X").enrolledLearnerIds().isEmpty());
        assertTrue(service.inspect("X").waitlist().isEmpty());
    }

    @Test void dropPromotesFifoAndRejectsNonEnrolledAndRepeatedDrops() {
        service.enroll("X", "L101"); service.enroll("X", "L104"); service.enroll("X", "L105");
        var result = service.drop("X", "L101");
        assertEquals("DROPPED", result.outcome());
        assertEquals("Enrollment dropped", result.message());
        assertEquals(List.of("L104"), service.inspect("X").enrolledLearnerIds());
        assertEquals(List.of("L105"), service.inspect("X").waitlist());
        assertThrows(ApiException.class, () -> service.drop("X", "L101"));
        assertEquals(List.of("L105"), service.inspect("X").waitlist());
        assertThrows(ApiException.class, () -> service.drop("X", "L106"));
    }

    @Test void skipsInactiveAtFrontRetainsPositionAndLeavesSeatWhenNoneEligible() {
        service.enroll("X", "L101"); service.enroll("X", "L104"); service.enroll("X", "L105");
        learners.setActiveStatus("L104", false);
        assertEquals("DROPPED", service.drop("X", "L101").outcome());
        assertEquals(List.of("L105"), service.inspect("X").enrolledLearnerIds());
        assertEquals(List.of("L104"), service.inspect("X").waitlist());
        service.drop("X", "L105");
        assertTrue(service.inspect("X").enrolledLearnerIds().isEmpty());
        assertEquals(1, service.inspect("X").availableSeats());
    }

    @Test void checksActivityAgainAfterWaitlisting() {
        service.enroll("X", "L101"); service.enroll("X", "L104");
        learners.setActiveStatus("L104", false);
        service.drop("X", "L101");
        assertFalse(service.inspect("X").enrolledLearnerIds().contains("L104"));
        assertEquals(List.of("L104"), service.inspect("X").waitlist());
    }

    @Test void unknownIdsAreErrors() {
        assertEquals("UNKNOWN_LEARNER", assertThrows(ApiException.class, () -> service.enroll("X", "bad")).code());
        assertEquals("UNKNOWN_OFFERING", assertThrows(ApiException.class, () -> service.enroll("bad", "L101")).code());
    }

    @Test void concurrentRequestsCannotExceedCapacityOrDuplicateEnrollment() throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        try {
            var gate = new CountDownLatch(1);
            List<java.util.concurrent.Future<?>> tasks = new ArrayList<>();
            String[] ids = {"L101", "L104", "L105", "L106", "L101", "L104", "L105", "L106"};
            for (String id : ids) tasks.add(executor.submit(() -> { gate.await(); service.enroll("X", id); return null; }));
            gate.countDown();
            for (var task : tasks) task.get();
        } finally { executor.shutdownNow(); }
        assertEquals(1, service.inspect("X").enrolledLearnerIds().size());
        assertEquals(3, service.inspect("X").waitlist().size());
        assertEquals(4, service.inspect("X").enrolledLearnerIds().size() + service.inspect("X").waitlist().size());
    }
}
