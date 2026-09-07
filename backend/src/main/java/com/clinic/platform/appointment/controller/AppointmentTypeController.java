package com.clinic.platform.appointment.controller;

import com.clinic.platform.appointment.dto.AppointmentTypeRequest;
import com.clinic.platform.appointment.dto.AppointmentTypeResponse;
import com.clinic.platform.appointment.service.AppointmentTypeService;
import com.clinic.platform.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointment-types")
// NOTE: reuses TENANT_MANAGE rather than introducing a new permission — appointment type
// configuration is tenant-wide policy on par with clinic setup, not routine day-to-day staff
// work. Revisit if a narrower SERVICE_MANAGE-style permission proves useful once real admin
// UIs exist.
public class AppointmentTypeController {

    private final AppointmentTypeService service;

    public AppointmentTypeController(AppointmentTypeService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TENANT_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AppointmentTypeResponse> create(@Valid @RequestBody AppointmentTypeRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    /** Authenticated (not public) — the patient booking UI needs this list to build the booking form. */
    @GetMapping
    public ApiResponse<List<AppointmentTypeResponse>> list() {
        return ApiResponse.ok(service.listActive());
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('TENANT_MANAGE')")
    public ApiResponse<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(id);
        return ApiResponse.ok(null);
    }
}
