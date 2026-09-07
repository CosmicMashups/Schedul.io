package com.clinic.platform.checkin.dto;

import com.clinic.platform.checkin.domain.CheckIn;

import java.time.Instant;
import java.util.UUID;

public record CheckInResponse(
        UUID id,
        UUID appointmentId,
        UUID patientId,
        Instant checkedInAt,
        String method,
        boolean identityVerified,
        UUID queueTicketId,
        String ticketNumber
) {
    public static CheckInResponse from(CheckIn c, UUID queueTicketId, String ticketNumber) {
        return new CheckInResponse(c.getId(), c.getAppointmentId(), c.getPatientId(), c.getCheckedInAt(),
                c.getMethod().name(), c.isIdentityVerified(), queueTicketId, ticketNumber);
    }
}
