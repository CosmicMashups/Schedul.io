package com.clinic.platform.patient.controller;

import com.clinic.platform.common.response.ApiResponse;
import com.clinic.platform.patient.domain.PatientConsent;
import com.clinic.platform.patient.dto.PatientRegistrationRequest;
import com.clinic.platform.patient.dto.PatientResponse;
import com.clinic.platform.patient.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    /**
     * Requires authentication (not in public-paths) even though it's "registration" — a
     * patient must at least be logged into the portal, or the call is being made by staff
     * during front-desk/phone/walk-in registration (section 6/28/29). Anonymous registration
     * is a product decision for Milestone 5's patient portal, not assumed here.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('PATIENT_WRITE', 'SELF_REGISTER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PatientResponse> register(@Valid @RequestBody PatientRegistrationRequest request) {
        return ApiResponse.ok(patientService.register(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PATIENT_READ')")
    public ApiResponse<PatientResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(patientService.get(id));
    }

    /**
     * Closes a gap flagged during the patient portal build: previously there was no way for
     * a returning, authenticated patient to resolve their own record. Requires SELF_REGISTER
     * (the patient-portal authority) rather than PATIENT_READ — a staff member's PATIENT_READ
     * grant is for looking up OTHER people's records, not "my own", and this endpoint has no
     * concept of "my own" for a staff account.
     */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('SELF_REGISTER')")
    public ApiResponse<PatientResponse> me() {
        return ApiResponse.ok(patientService.getMe());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PATIENT_READ')")
    public ApiResponse<List<PatientResponse>> search(@RequestParam String query) {
        return ApiResponse.ok(patientService.search(query));
    }

    @PostMapping("/{id}/consents/{type}/withdraw")
    @PreAuthorize("hasAuthority('PATIENT_WRITE')")
    public ApiResponse<Void> withdrawConsent(@PathVariable UUID id, @PathVariable PatientConsent.ConsentType type) {
        patientService.withdrawConsent(id, type);
        return ApiResponse.ok(null);
    }
}
