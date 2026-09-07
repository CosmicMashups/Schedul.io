package com.clinic.platform.clinic.dto;

import com.clinic.platform.clinic.domain.Clinic;

import java.util.UUID;

public record ClinicResponse(
        UUID id,
        String name,
        String addressLine,
        String city,
        String province,
        String postalCode,
        String contactNumber,
        String timezone,
        String status
) {
    public static ClinicResponse from(Clinic c) {
        return new ClinicResponse(c.getId(), c.getName(), c.getAddressLine(), c.getCity(),
                c.getProvince(), c.getPostalCode(), c.getContactNumber(), c.getTimezone(),
                c.getStatus().name());
    }
}
