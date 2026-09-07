package com.clinic.platform.practitioner.dto;

import com.clinic.platform.practitioner.domain.Practitioner;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PractitionerResponse(
        UUID id,
        String firstName,
        String lastName,
        String credentials,
        BigDecimal defaultConsultationFee,
        String status,
        List<String> specialties,
        List<UUID> specialtyIds,
        List<String> clinics,
        List<UUID> clinicIds
) {
    public static PractitionerResponse from(Practitioner p) {
        return new PractitionerResponse(
                p.getId(), p.getFirstName(), p.getLastName(), p.getCredentials(),
                p.getDefaultConsultationFee(), p.getStatus().name(),
                p.getSpecialties().stream().map(s -> s.getName()).toList(),
                p.getSpecialties().stream().map(s -> s.getId()).toList(),
                p.getClinics().stream().map(c -> c.getName()).toList(),
                p.getClinics().stream().map(c -> c.getId()).toList()
        );
    }
}
