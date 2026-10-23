package com.vinay.aiassistance.courseregistration.dto;

import java.time.Instant;

public record ApiErrorResponse(String code, String message, Instant timestamp) {
}
