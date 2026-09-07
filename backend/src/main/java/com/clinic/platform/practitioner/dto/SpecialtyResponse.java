package com.clinic.platform.practitioner.dto;

import com.clinic.platform.practitioner.domain.Specialty;

import java.util.UUID;

public record SpecialtyResponse(UUID id, String code, String name) {
    public static SpecialtyResponse from(Specialty s) {
        return new SpecialtyResponse(s.getId(), s.getCode(), s.getName());
    }
}
