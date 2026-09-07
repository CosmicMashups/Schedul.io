package com.clinic.platform.practitioner.dto;

import jakarta.validation.constraints.NotBlank;

public record SpecialtyRequest(@NotBlank String code, @NotBlank String name) {
}
