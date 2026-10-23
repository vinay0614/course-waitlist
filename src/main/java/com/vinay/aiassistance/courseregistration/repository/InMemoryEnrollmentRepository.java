package com.vinay.aiassistance.courseregistration.repository;

import com.vinay.aiassistance.courseregistration.model.CourseOffering;
import com.vinay.aiassistance.courseregistration.model.EnrollmentState;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryEnrollmentRepository {
    private final Map<String, EnrollmentState> offerings = new ConcurrentHashMap<>();

    public InMemoryEnrollmentRepository() {
        this(new CourseOffering("JAVA-ADV-2026-SEM1", "JAVA-FUNDAMENTALS", 2));
    }

    public InMemoryEnrollmentRepository(CourseOffering offering) {
        offerings.put(offering.offeringId(), new EnrollmentState(offering));
    }

    public Optional<EnrollmentState> findById(String id) {
        return Optional.ofNullable(offerings.get(id));
    }
}
