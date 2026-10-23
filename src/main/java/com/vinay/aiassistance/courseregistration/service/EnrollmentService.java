package com.vinay.aiassistance.courseregistration.service;

import com.vinay.aiassistance.courseregistration.dto.OperationResponse;
import com.vinay.aiassistance.courseregistration.dto.OfferingResponse;
import com.vinay.aiassistance.courseregistration.exception.ApiException;
import com.vinay.aiassistance.courseregistration.model.EnrollmentState;
import com.vinay.aiassistance.courseregistration.repository.InMemoryEnrollmentRepository;
import com.vinay.aiassistance.courseregistration.repository.MockLearnerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class EnrollmentService {
    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);
    private final InMemoryEnrollmentRepository enrollmentRepository;
    private final MockLearnerRepository learnerRepository;

    public EnrollmentService(InMemoryEnrollmentRepository enrollmentRepository, MockLearnerRepository learnerRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.learnerRepository = learnerRepository;
    }

    public synchronized OperationResponse enroll(String offeringId, String learnerId) {
        EnrollmentState state = offering(offeringId);
        var learner = learnerRepository.findById(learnerId).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "UNKNOWN_LEARNER", "Learner not found: " + learnerId));
        if (state.contains(learnerId)) return rejected("DUPLICATE", "Learner is already enrolled or waitlisted");
        if (!learner.hasCompleted(state.offering().prerequisite()))
            return rejected("REJECTED_PREREQUISITE", "Required prerequisite is incomplete");
        if (!learner.activeStudent()) return rejected("REJECTED_INACTIVE", "Learner is not an active student");
        if (state.availableSeats() > 0) {
            state.enroll(learnerId);
            log.info("Enrollment completed with outcome={}", "ENROLLED");
            return new OperationResponse("ENROLLED", "Learner enrolled");
        }
        state.waitlist(learnerId);
        log.info("Eligible learner added to waitlist with outcome={}", "WAITLISTED");
        return new OperationResponse("WAITLISTED", "Course is full; learner added to FIFO waitlist");
    }

    public synchronized OperationResponse drop(String offeringId, String learnerId) {
        EnrollmentState state = offering(offeringId);
        if (!state.removeEnrolled(learnerId)) {
            log.warn("Enrollment drop rejected with outcome={}", "NOT_ENROLLED");
            throw error(HttpStatus.CONFLICT, "NOT_ENROLLED", "Learner is not enrolled in this offering");
        }
        boolean promoted = false;
        for (String candidateId : state.waitlist()) {
            var candidate = learnerRepository.findById(candidateId).orElse(null);
            if (candidate != null && candidate.activeStudent() && candidate.hasCompleted(state.offering().prerequisite())) {
                state.removeWaitlisted(candidateId);
                state.enroll(candidateId);
                promoted = true;
                break;
            }
        }
        log.info("Enrollment dropped with outcome={}", "DROPPED");
        if (promoted) log.info("Waitlisted learner promoted after drop");
        else log.info("Drop completed with no eligible waitlisted learner");
        return new OperationResponse("DROPPED", "Enrollment dropped");
    }

    public synchronized OfferingResponse inspect(String offeringId) {
        EnrollmentState state = offering(offeringId);
        return new OfferingResponse(offeringId, state.offering().capacity(), List.copyOf(state.enrolled()), state.availableSeats(), List.copyOf(state.waitlist()));
    }

    private EnrollmentState offering(String id) {
        return enrollmentRepository.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "UNKNOWN_OFFERING", "Offering not found: " + id));
    }

    private OperationResponse rejected(String outcome, String message) {
        log.warn("Enrollment request rejected with outcome={}", outcome);
        return new OperationResponse(outcome, message);
    }

    private ApiException error(HttpStatus status, String code, String message) {
        return new ApiException(status, code, message);
    }
}
