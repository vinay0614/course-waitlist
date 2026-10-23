package com.vinay.aiassistance.courseregistration.model;

public record CourseOffering(String offeringId, String prerequisite, int capacity) {
    public CourseOffering {
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive");
    }
}
