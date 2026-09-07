package com.clinic.platform.appointment.dto;

import com.clinic.platform.appointment.domain.Appointment;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID patientId,
        UUID practitionerId,
        UUID clinicId,
        UUID serviceId,
        UUID appointmentTypeId,
        UUID slotId,
        Instant scheduledStart,
        Instant scheduledEnd,
        String status,
        String reason,
        String source,
        UUID previousAppointmentId,
        UUID replacementAppointmentId,
        String cancellationReason,
        String cancellationNote
) {
    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.getId(), a.getPatientId(), a.getPractitionerId(), a.getClinicId(), a.getServiceId(),
                a.getAppointmentTypeId(), a.getSlotId(), a.getScheduledStart(), a.getScheduledEnd(),
                a.getStatus().name(), a.getReason(), a.getSource().name(),
                a.getPreviousAppointmentId(), a.getReplacementAppointmentId(),
                a.getCancellationReason() != null ? a.getCancellationReason().name() : null,
                a.getCancellationNote()
        );
    }
}
