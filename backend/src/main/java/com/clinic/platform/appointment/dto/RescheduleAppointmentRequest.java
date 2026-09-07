package com.clinic.platform.appointment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RescheduleAppointmentRequest(
        @NotNull UUID newSlotId,
        String reason
) {
}
