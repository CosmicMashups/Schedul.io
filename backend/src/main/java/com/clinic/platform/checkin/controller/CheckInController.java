package com.clinic.platform.checkin.controller;

import com.clinic.platform.checkin.dto.CheckInRequest;
import com.clinic.platform.checkin.dto.CheckInResponse;
import com.clinic.platform.checkin.service.CheckInService;
import com.clinic.platform.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    /**
     * Section 39's staff API surface (POST /staff/appointments/{id}/check-in). Also covers
     * section 20's QR/kiosk/portal self-check-in methods via the `method` field in the
     * request body — the endpoint itself doesn't need to be public for those, since a patient
     * using a QR code or portal is still an authenticated session by the time this is called.
     */
    @PostMapping("/api/v1/staff/appointments/{id}/check-in")
    @PreAuthorize("hasAuthority('CHECKIN_MANAGE')")
    public ApiResponse<CheckInResponse> checkIn(@PathVariable UUID id, @Valid @RequestBody CheckInRequest request) {
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.ok(checkInService.checkIn(id, actor, request));
    }
}
