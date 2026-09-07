package com.clinic.platform.scheduling.controller;

import com.clinic.platform.common.response.ApiResponse;
import com.clinic.platform.scheduling.dto.ScheduleExceptionRequest;
import com.clinic.platform.scheduling.dto.ScheduleRuleRequest;
import com.clinic.platform.scheduling.dto.ScheduleRuleResponse;
import com.clinic.platform.scheduling.service.ScheduleManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
@PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
public class ScheduleController {

    private final ScheduleManagementService scheduleManagementService;

    public ScheduleController(ScheduleManagementService scheduleManagementService) {
        this.scheduleManagementService = scheduleManagementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ScheduleRuleResponse> createRule(@Valid @RequestBody ScheduleRuleRequest request) {
        return ApiResponse.ok(scheduleManagementService.createRule(request));
    }

    @GetMapping
    public ApiResponse<List<ScheduleRuleResponse>> listForPractitioner(@RequestParam UUID practitionerId) {
        return ApiResponse.ok(scheduleManagementService.listRulesForPractitioner(practitionerId));
    }

    @PostMapping("/{ruleId}/deactivate")
    public ApiResponse<Void> deactivateRule(@PathVariable UUID ruleId) {
        scheduleManagementService.deactivateRule(ruleId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createException(@Valid @RequestBody ScheduleExceptionRequest request) {
        scheduleManagementService.createException(request);
        return ApiResponse.ok(null);
    }
}
