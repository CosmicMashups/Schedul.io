package com.clinic.platform.appointment.dto;

import com.clinic.platform.appointment.domain.Appointment;
import jakarta.validation.constraints.NotNull;

public record CancelAppointmentRequest(
        @NotNull Appointment.CancellationReason reason,
        String note
) {
}
