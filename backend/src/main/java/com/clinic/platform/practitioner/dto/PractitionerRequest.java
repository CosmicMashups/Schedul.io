package com.clinic.platform.practitioner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record PractitionerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String credentials,
        BigDecimal defaultConsultationFee,
        @NotEmpty Set<UUID> specialtyIds,
        @NotEmpty Set<UUID> clinicIds,
        UUID userId // optional: links this practitioner to a Doctor Portal login account
) {
}
