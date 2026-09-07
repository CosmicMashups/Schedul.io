package com.clinic.platform.clinic.controller;

import com.clinic.platform.clinic.dto.ClinicRequest;
import com.clinic.platform.clinic.dto.ClinicResponse;
import com.clinic.platform.clinic.service.ClinicService;
import com.clinic.platform.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff/clinics")
public class ClinicController {

    private final ClinicService clinicService;

    public ClinicController(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TENANT_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClinicResponse> create(@Valid @RequestBody ClinicRequest request) {
        return ApiResponse.ok(clinicService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TENANT_MANAGE')")
    public ApiResponse<ClinicResponse> update(@PathVariable UUID id, @Valid @RequestBody ClinicRequest request) {
        return ApiResponse.ok(clinicService.update(id, request));
    }

    @GetMapping
    public ApiResponse<List<ClinicResponse>> list() {
        return ApiResponse.ok(clinicService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ClinicResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(clinicService.get(id));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('TENANT_MANAGE')")
    public ApiResponse<Void> deactivate(@PathVariable UUID id) {
        clinicService.deactivate(id);
        return ApiResponse.ok(null);
    }
}
