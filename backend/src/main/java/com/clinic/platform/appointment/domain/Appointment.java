package com.clinic.platform.appointment.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Section 4: Appointment ≠ Slot ≠ Visit. This table is the PLAN (who, what, when, with whom,
 * under what policy); {@link com.clinic.platform.scheduling.domain.Slot} is the calendar
 * reservation it consumes; the eventual Encounter/Visit (Milestone 7) is what actually
 * happened when the patient showed up. Deliberately does not reuse Slot's status enum — an
 * Appointment can be CANCELLED while its Slot is independently released back to FREE; they
 * are related but not the same lifecycle (this is the single most important modeling
 * decision in the whole system per the original architecture review, section 67).
 *
 * previousAppointmentId/replacementAppointmentId implement section 17: a reschedule creates a
 * NEW Appointment row rather than mutating the old one's time, so the audit trail shows the
 * full history rather than overwriting it.
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
public class Appointment extends TenantScopedEntity {

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "practitioner_id", nullable = false)
    private UUID practitionerId;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "appointment_type_id", nullable = false)
    private UUID appointmentTypeId;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "scheduled_start", nullable = false)
    private Instant scheduledStart;

    @Column(name = "scheduled_end", nullable = false)
    private Instant scheduledEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.REQUESTED;

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentSource source = AppointmentSource.ONLINE;

    // ---- Reschedule chain (section 17) ----
    @Column(name = "previous_appointment_id")
    private UUID previousAppointmentId;

    @Column(name = "replacement_appointment_id")
    private UUID replacementAppointmentId;

    // ---- Cancellation (section 18) ----
    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason")
    private CancellationReason cancellationReason;

    @Column(name = "cancellation_note")
    private String cancellationNote;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    public enum AppointmentStatus {
        REQUESTED, PENDING_CONFIRMATION, CONFIRMED,
        CHECKED_IN, IN_QUEUE, IN_CONSULTATION, COMPLETED,   // driven by later milestones (6/7); enum defined now to avoid another migration
        REJECTED, CANCELLED, RESCHEDULED, NO_SHOW, LEFT_WITHOUT_BEING_SEEN
    }

    /** Section 28 concept, kept as its own enum (not shared with patient.RegistrationSource) so the two modules stay decoupled even though values currently overlap. */
    public enum AppointmentSource {
        ONLINE, PHONE, FRONT_DESK, WALK_IN, REFERRAL, STAFF
    }

    public enum CancellationReason {
        PATIENT_REQUEST, DOCTOR_UNAVAILABLE, CLINIC_CLOSED, DUPLICATE, EMERGENCY, NO_LONGER_NEEDED, OTHER
    }
}
