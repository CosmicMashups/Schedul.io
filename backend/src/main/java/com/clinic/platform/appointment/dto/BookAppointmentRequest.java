package com.clinic.platform.appointment.dto;

import com.clinic.platform.appointment.domain.Appointment;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BookAppointmentRequest(
        @NotNull UUID patientId,
        @NotNull UUID practitionerId,
        @NotNull UUID clinicId,
        @NotNull UUID serviceId,
        @NotNull UUID appointmentTypeId,
        @NotNull UUID slotId,
        String reason,
        Appointment.AppointmentSource source
) {
}
