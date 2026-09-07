package com.clinic.platform.queue.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Section 22-23: appointmentId is nullable — a walk-in (section 28) gets a QueueTicket
 * without ever having had an Appointment. ticketNumber is a display string like "A-023"
 * (section 22's example), generated per-queue-per-day by {@link com.clinic.platform.queue.service.QueueService};
 * priority is an int rather than a hardcoded enum ranking so section 23's "configurable and
 * transparent" priority policy can be tuned without a code change (lower number = higher
 * priority, matching the section 23 ordering EMERGENCY(1) > ELDERLY(2) > SCHEDULED(3) > WALK_IN(4)).
 */
@Entity
@Table(name = "queue_tickets")
@Getter
@Setter
public class QueueTicket extends TenantScopedEntity {

    @Column(name = "queue_id", nullable = false)
    private UUID queueId;

    @Column(name = "appointment_id")
    private UUID appointmentId; // nullable: walk-ins have no appointment

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "practitioner_id")
    private UUID practitionerId; // nullable until assigned (e.g. registration queue)

    @Column(name = "ticket_number", nullable = false)
    private String ticketNumber;

    @Column(nullable = false)
    private int priority = 3; // section 23 default: SCHEDULED tier

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.WAITING;

    @Column(name = "called_at")
    private Instant calledAt;

    @Column(name = "serving_started_at")
    private Instant servingStartedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public enum TicketStatus {
        WAITING, CALLED, SERVING, COMPLETED, SKIPPED, NO_SHOW, CANCELLED
    }
}
