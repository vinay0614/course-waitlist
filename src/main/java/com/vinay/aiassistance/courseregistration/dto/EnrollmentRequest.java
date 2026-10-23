package com.vinay.aiassistance.courseregistration.dto;

import jakarta.validation.constraints.NotBlank;

public record EnrollmentRequest(@NotBlank String learnerId) {
}
