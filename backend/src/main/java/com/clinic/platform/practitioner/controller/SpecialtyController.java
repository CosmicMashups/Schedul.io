package com.clinic.platform.practitioner.controller;

import com.clinic.platform.common.response.ApiResponse;
import com.clinic.platform.practitioner.dto.SpecialtyRequest;
import com.clinic.platform.practitioner.dto.SpecialtyResponse;
import com.clinic.platform.practitioner.service.SpecialtyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialties")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    public SpecialtyController(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
    }

    /** Authenticated (not public) — matches the doctor directory's own auth posture; a picker only needs to work inside the staff console. */
    @GetMapping
    public ApiResponse<List<SpecialtyResponse>> list() {
        return ApiResponse.ok(specialtyService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRACTITIONER_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SpecialtyResponse> create(@Valid @RequestBody SpecialtyRequest request) {
        return ApiResponse.ok(specialtyService.create(request));
    }
}
