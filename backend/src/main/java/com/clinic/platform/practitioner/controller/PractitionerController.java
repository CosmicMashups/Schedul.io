package com.clinic.platform.practitioner.controller;

import com.clinic.platform.common.response.ApiResponse;
import com.clinic.platform.practitioner.dto.LinkUserRequest;
import com.clinic.platform.practitioner.dto.PractitionerRequest;
import com.clinic.platform.practitioner.dto.PractitionerResponse;
import com.clinic.platform.practitioner.service.PractitionerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class PractitionerController {

    private final PractitionerService practitionerService;

    public PractitionerController(PractitionerService practitionerService) {
        this.practitionerService = practitionerService;
    }

    // ---- Staff management ----

    @PostMapping("/api/v1/staff/practitioners")
    @PreAuthorize("hasAuthority('PRACTITIONER_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PractitionerResponse> create(@Valid @RequestBody PractitionerRequest request) {
        return ApiResponse.ok(practitionerService.create(request));
    }

    @PutMapping("/api/v1/staff/practitioners/{id}")
    @PreAuthorize("hasAuthority('PRACTITIONER_MANAGE')")
    public ApiResponse<PractitionerResponse> update(@PathVariable UUID id, @Valid @RequestBody PractitionerRequest request) {
        return ApiResponse.ok(practitionerService.update(id, request));
    }

    @PostMapping("/api/v1/staff/practitioners/{id}/leave")
    @PreAuthorize("hasAuthority('PRACTITIONER_MANAGE')")
    public ApiResponse<Void> markOnLeave(@PathVariable UUID id) {
        practitionerService.setOnLeave(id, true);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/v1/staff/practitioners/{id}/return-from-leave")
    @PreAuthorize("hasAuthority('PRACTITIONER_MANAGE')")
    public ApiResponse<Void> returnFromLeave(@PathVariable UUID id) {
        practitionerService.setOnLeave(id, false);
        return ApiResponse.ok(null);
    }

    /** Doctor Portal "invite" flow, step 2: link a just-created (or existing) User account as this practitioner's portal login. */
    @PostMapping("/api/v1/staff/practitioners/{id}/link-user")
    @PreAuthorize("hasAuthority('PRACTITIONER_MANAGE')")
    public ApiResponse<PractitionerResponse> linkUser(@PathVariable UUID id, @Valid @RequestBody LinkUserRequest request) {
        return ApiResponse.ok(practitionerService.linkUser(id, request.userId()));
    }

    // ---- Public / patient-facing directory (section 7) ----

    @GetMapping("/api/v1/doctors")
    public ApiResponse<List<PractitionerResponse>> search(
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) UUID clinicId,
            @RequestParam(required = false) String name) {
        return ApiResponse.ok(practitionerService.search(specialty, clinicId, name));
    }

    /** New for the Doctor Portal: resolves the practitioner record linked to the calling account. */
    @GetMapping("/api/v1/doctors/me")
    @PreAuthorize("hasAuthority('DOCTOR_PORTAL_ACCESS')")
    public ApiResponse<PractitionerResponse> me() {
        return ApiResponse.ok(practitionerService.getMe());
    }

    @GetMapping("/api/v1/doctors/{id}")
    public ApiResponse<PractitionerResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(practitionerService.get(id));
    }
}
