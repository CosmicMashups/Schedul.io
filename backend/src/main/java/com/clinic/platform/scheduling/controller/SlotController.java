package com.clinic.platform.scheduling.controller;

import com.clinic.platform.common.response.ApiResponse;
import com.clinic.platform.scheduling.dto.SlotResponse;
import com.clinic.platform.scheduling.service.SlotHoldService;
import com.clinic.platform.security.CurrentUser;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class SlotController {

    private final SlotHoldService slotHoldService;

    public SlotController(SlotHoldService slotHoldService) {
        this.slotHoldService = slotHoldService;
    }

    /** Section 12: patient/staff selects a slot in the booking UI — holds it for a short window. Requires auth. */
    @PostMapping("/api/v1/slots/{id}/hold")
    public ApiResponse<SlotResponse> hold(@PathVariable UUID id) {
        var slot = slotHoldService.hold(id, CurrentUser.require());
        return ApiResponse.ok(SlotResponse.from(slot));
    }

    /** Explicit release if the patient backs out of the booking form before the hold expires. */
    @PostMapping("/api/v1/slots/{id}/release")
    public ApiResponse<Void> release(@PathVariable UUID id) {
        slotHoldService.release(id, CurrentUser.require());
        return ApiResponse.ok(null);
    }
}
