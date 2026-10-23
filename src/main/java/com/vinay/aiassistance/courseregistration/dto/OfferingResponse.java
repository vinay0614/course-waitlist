package com.vinay.aiassistance.courseregistration.dto;

import java.util.List;

public record OfferingResponse(String offeringId, int capacity, List<String> enrolledLearnerIds,
                               int availableSeats, List<String> waitlist) {
}
