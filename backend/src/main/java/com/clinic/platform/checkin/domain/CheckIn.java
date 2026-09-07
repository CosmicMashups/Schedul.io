package com.clinic.platform.checkin.domain;

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
 * Section 20: the moment an Appointment (a plan) becomes an active visit-in-progress. This is
 * intentionally its own row rather than just a timestamp column on Appointment — a full
 * Encounter/Visit entity (section 34's visits table, tracking consultation start/end,
 * diagnosis-adjacent metadata) is deferred to a later milestone; for now CheckIn +
 * Appointment.status (CHECKED_IN → IN_QUEUE → IN_CONSULTATION → COMPLETED, already declared
 * in Milestone 4) carry the patient-flow state that Milestone 7 needs. Revisit once clinical
 * documentation requirements make a dedicated Visit/Encounter table necessary.
 */
@Entity
@Table(name = "check_ins")
@Getter
@Setter
public class CheckIn extends TenantScopedEntity {

    @Column(name = "appointment_id", nullable = false, unique = true)
    private UUID appointmentId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckInMethod method;

    @Column(name = "identity_verified", nullable = false)
    private boolean identityVerified;

    @Column(name = "checked_in_by")
    private String checkedInBy; // staff user display name, or "self" for kiosk/portal/QR

    public enum CheckInMethod {
        RECEPTIONIST, QR_CODE, PATIENT_PORTAL, KIOSK, MOBILE_APP
    }
}
