package com.clinic.platform.catalog.controller;

import com.clinic.platform.catalog.dto.ServiceRequest;
import com.clinic.platform.catalog.dto.ServiceResponse;
import com.clinic.platform.catalog.service.ServiceCatalogService;
import com.clinic.platform.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    public ServiceCatalogController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @PostMapping("/api/v1/staff/services")
    @PreAuthorize("hasAuthority('SERVICE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ServiceResponse> create(@Valid @RequestBody ServiceRequest request) {
        return ApiResponse.ok(serviceCatalogService.create(request));
    }

    @PutMapping("/api/v1/staff/services/{id}")
    @PreAuthorize("hasAuthority('SERVICE_MANAGE')")
    public ApiResponse<ServiceResponse> update(@PathVariable UUID id, @Valid @RequestBody ServiceRequest request) {
        return ApiResponse.ok(serviceCatalogService.update(id, request));
    }

    @PostMapping("/api/v1/staff/services/{id}/deactivate")
    @PreAuthorize("hasAuthority('SERVICE_MANAGE')")
    public ApiResponse<Void> deactivate(@PathVariable UUID id) {
        serviceCatalogService.deactivate(id);
        return ApiResponse.ok(null);
    }

    // Public/patient-facing (section 8)
    @GetMapping("/api/v1/services")
    public ApiResponse<List<ServiceResponse>> list(@RequestParam(required = false) UUID clinicId) {
        return ApiResponse.ok(serviceCatalogService.listActive(clinicId));
    }

    @GetMapping("/api/v1/services/{id}")
    public ApiResponse<ServiceResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(serviceCatalogService.get(id));
    }
}
