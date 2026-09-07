package com.clinic.platform.appointment.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Section 33: "Keep the audit/event timeline separate from the clinical record." This is a
 * narrower, purpose-built table for rendering an appointment's lifecycle (REQUESTED →
 * CONFIRMED → CHECKED_IN → ...) directly in a patient/staff-facing UI, distinct from the
 * generic {@link com.clinic.platform.audit.domain.AuditLog}, which exists for compliance
 * review across every entity type in the system, not fast per-appointment timeline reads.
 */
@Entity
@Table(name = "appointment_status_history")
@Getter
@Setter
public class AppointmentStatusHistory extends TenantScopedEntity {

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private Appointment.AppointmentStatus fromStatus; // null for the initial REQUESTED/CONFIRMED row

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private Appointment.AppointmentStatus toStatus;

    @Column(name = "reason")
    private String reason;
}
