package com.clinic.platform.catalog.dto;

import com.clinic.platform.catalog.domain.ClinicService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ServiceRequest(
        @NotBlank String name,
        String description,
        @Min(5) int durationMinutes,
        @Min(0) int bufferMinutes,
        BigDecimal price,
        ClinicService.ConsultationMode consultationMode,
        UUID allowedSpecialtyId,
        @NotEmpty Set<UUID> clinicIds
) {
}
