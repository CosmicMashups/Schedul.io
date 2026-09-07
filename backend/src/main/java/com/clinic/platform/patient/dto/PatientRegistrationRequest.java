package com.clinic.platform.patient.dto;

import com.clinic.platform.patient.domain.Patient;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record PatientRegistrationRequest(
        @NotBlank String firstName,
        String middleName,
        @NotBlank String lastName,
        String suffix,
        @NotNull @Past LocalDate birthDate,
        @NotNull Patient.Sex sex,
        String mobileNumber,
        String email,
        String addressLine,
        String emergencyContactName,
        String emergencyContactNumber,
        Patient.ContactMethod preferredContactMethod,
        Patient.RegistrationSource registrationSource,
        String privacyNoticeVersion,
        @AssertTrue(message = "Data processing consent is required to register a patient record.")
        boolean dataProcessingConsentGranted
) {
}
