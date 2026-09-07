package com.clinic.platform.queue.dto;

import com.clinic.platform.queue.domain.QueueTicket;

import java.time.Instant;
import java.util.UUID;

public record QueueTicketResponse(
        UUID id,
        UUID queueId,
        UUID appointmentId,
        UUID patientId,
        UUID practitionerId,
        String ticketNumber,
        int priority,
        String status,
        Instant calledAt,
        Instant servingStartedAt,
        Instant completedAt
) {
    public static QueueTicketResponse from(QueueTicket t) {
        return new QueueTicketResponse(t.getId(), t.getQueueId(), t.getAppointmentId(), t.getPatientId(),
                t.getPractitionerId(), t.getTicketNumber(), t.getPriority(), t.getStatus().name(),
                t.getCalledAt(), t.getServingStartedAt(), t.getCompletedAt());
    }
}
