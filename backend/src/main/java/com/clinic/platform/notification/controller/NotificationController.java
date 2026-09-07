package com.clinic.platform.notification.controller;

import com.clinic.platform.common.response.ApiResponse;
import com.clinic.platform.notification.dto.NotificationResponse;
import com.clinic.platform.notification.dto.NotificationTemplateRequest;
import com.clinic.platform.notification.dto.NotificationTemplateResponse;
import com.clinic.platform.notification.service.NotificationTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
// NOTE: reuses TENANT_MANAGE for template authoring (same reasoning as AppointmentTypeController -
// notification templates are tenant-wide configuration, not routine staff work) and AUDIT_VIEW
// for reading delivery history, since "did this patient actually get notified" is an audit-style
// question rather than day-to-day patient management.
public class NotificationController {

    private final NotificationTemplateService templateService;

    public NotificationController(NotificationTemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping("/api/v1/notification-templates")
    @PreAuthorize("hasAuthority('TENANT_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NotificationTemplateResponse> create(@Valid @RequestBody NotificationTemplateRequest request) {
        return ApiResponse.ok(templateService.create(request));
    }

    @GetMapping("/api/v1/notification-templates")
    @PreAuthorize("hasAuthority('TENANT_MANAGE')")
    public ApiResponse<List<NotificationTemplateResponse>> list() {
        return ApiResponse.ok(templateService.list());
    }

    @GetMapping("/api/v1/staff/patients/{patientId}/notifications")
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public ApiResponse<List<NotificationResponse>> historyForPatient(@PathVariable UUID patientId) {
        return ApiResponse.ok(templateService.historyForPatient(patientId));
    }
}
