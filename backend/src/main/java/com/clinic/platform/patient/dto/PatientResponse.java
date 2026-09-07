package com.clinic.platform.patient.dto;

import com.clinic.platform.patient.domain.Patient;

import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String firstName,
        String middleName,
        String lastName,
        String suffix,
        LocalDate birthDate,
        String sex,
        String mobileNumber,
        String email,
        String addressLine,
        String preferredContactMethod
) {
    public static PatientResponse from(Patient p) {
        return new PatientResponse(
                p.getId(), p.getFirstName(), p.getMiddleName(), p.getLastName(), p.getSuffix(),
                p.getBirthDate(), p.getSex().name(), p.getMobileNumber(), p.getEmail(),
                p.getAddressLine(), p.getPreferredContactMethod().name()
        );
    }
}
