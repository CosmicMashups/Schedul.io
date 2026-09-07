package com.clinic.platform.appointment.dto;

import com.clinic.platform.appointment.domain.AppointmentType;

import java.util.UUID;

public record AppointmentTypeResponse(
        UUID id,
        String code,
        String name,
        String confirmationPolicy,
        Integer cancellationWindowHours,
        Integer advanceBookingLimitDays,
        boolean active
) {
    public static AppointmentTypeResponse from(AppointmentType t) {
        return new AppointmentTypeResponse(t.getId(), t.getCode(), t.getName(),
                t.getConfirmationPolicy().name(), t.getCancellationWindowHours(),
                t.getAdvanceBookingLimitDays(), t.isActive());
    }
}
