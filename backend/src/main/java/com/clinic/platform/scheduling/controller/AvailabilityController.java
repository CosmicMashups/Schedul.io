package com.clinic.platform.scheduling.controller;

import com.clinic.platform.common.exception.ApiException;
import com.clinic.platform.common.response.ApiResponse;
import com.clinic.platform.scheduling.dto.AvailabilityResponse;
import com.clinic.platform.scheduling.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Public (no login required — see clinic.security.public-paths) but still tenant-scoped via
 * X-Tenant-Id, matching section 3's patient-side flow: browse availability before
 * authenticating for the actual booking step (Milestone 4).
 */
@RestController
public class AvailabilityController {

    private static final int MAX_RANGE_DAYS = 90;

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/api/v1/availability")
    public ApiResponse<AvailabilityResponse> getAvailability(
            @RequestParam UUID practitionerId,
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) UUID clinicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (to.isBefore(from)) {
            throw ApiException.badRequest("INVALID_RANGE", "'to' must not be before 'from'.");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw ApiException.badRequest("RANGE_TOO_WIDE", "Availability range cannot exceed " + MAX_RANGE_DAYS + " days.");
        }

        return ApiResponse.ok(availabilityService.getAvailability(practitionerId, serviceId, clinicId, from, to));
    }
}
