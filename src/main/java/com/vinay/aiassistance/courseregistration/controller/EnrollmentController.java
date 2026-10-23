package com.vinay.aiassistance.courseregistration.controller;

import com.vinay.aiassistance.courseregistration.dto.EnrollmentRequest;
import com.vinay.aiassistance.courseregistration.dto.OperationResponse;
import com.vinay.aiassistance.courseregistration.dto.OfferingResponse;
import com.vinay.aiassistance.courseregistration.service.EnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/offerings/{offeringId}")
public class EnrollmentController {
    private final EnrollmentService service;

    public EnrollmentController(EnrollmentService service) {
        this.service = service;
    }

    @PostMapping("/enroll")
    public OperationResponse enroll(@PathVariable @NotBlank String offeringId, @Valid @RequestBody EnrollmentRequest request) {
        return service.enroll(offeringId, request.learnerId());
    }

    @DeleteMapping("/enrollments/{learnerId}")
    public OperationResponse drop(@PathVariable @NotBlank String offeringId, @PathVariable @NotBlank String learnerId) {
        return service.drop(offeringId, learnerId);
    }

    @GetMapping
    public OfferingResponse inspect(@PathVariable @NotBlank String offeringId) {
        return service.inspect(offeringId);
    }
}
