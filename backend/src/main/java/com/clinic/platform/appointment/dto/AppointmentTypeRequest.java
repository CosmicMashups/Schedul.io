package com.clinic.platform.appointment.dto;

import com.clinic.platform.appointment.domain.AppointmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AppointmentTypeRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull AppointmentType.ConfirmationPolicy confirmationPolicy,
        Integer cancellationWindowHours,
        Integer advanceBookingLimitDays
) {
}
