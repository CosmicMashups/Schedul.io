package com.clinic.platform.catalog.dto;

import com.clinic.platform.catalog.domain.ClinicService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ServiceResponse(
        UUID id,
        String name,
        String description,
        int durationMinutes,
        int bufferMinutes,
        BigDecimal price,
        String consultationMode,
        String allowedSpecialty,
        UUID allowedSpecialtyId,
        String status,
        List<String> clinics,
        List<UUID> clinicIds
) {
    public static ServiceResponse from(ClinicService s) {
        return new ServiceResponse(
                s.getId(), s.getName(), s.getDescription(), s.getDurationMinutes(), s.getBufferMinutes(),
                s.getPrice(), s.getConsultationMode().name(),
                s.getAllowedSpecialty() != null ? s.getAllowedSpecialty().getName() : null,
                s.getAllowedSpecialty() != null ? s.getAllowedSpecialty().getId() : null,
                s.getStatus().name(),
                s.getClinics().stream().map(c -> c.getName()).toList(),
                s.getClinics().stream().map(c -> c.getId()).toList()
        );
    }
}
