package com.clinic.platform.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record ClinicRequest(
        @NotBlank String name,
        @NotBlank String addressLine,
        String city,
        String province,
        String postalCode,
        String contactNumber,
        String timezone
) {
}
