package com.clinic.platform.appointment.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Section 15/16: NEW_PATIENT, FOLLOW_UP, ROUTINE, URGENT, etc. are seed data (V5 migration),
 * not a hardcoded enum — a tenant admin can add more. confirmationPolicy is the field that
 * makes booking configurable per section 16 rather than one hardcoded workflow:
 * INSTANT_CONFIRMATION books straight to CONFIRMED; the two approval policies book to
 * PENDING_CONFIRMATION and require an explicit staff/doctor confirm step.
 */
@Entity
@Table(name = "appointment_types", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "code"}))
@Getter
@Setter
public class AppointmentType extends TenantScopedEntity {

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmation_policy", nullable = false)
    private ConfirmationPolicy confirmationPolicy = ConfirmationPolicy.INSTANT_CONFIRMATION;

    @Column(name = "cancellation_window_hours")
    private Integer cancellationWindowHours; // null = no restriction

    @Column(name = "advance_booking_limit_days")
    private Integer advanceBookingLimitDays; // null = no restriction

    @Column(nullable = false)
    private boolean active = true;

    public enum ConfirmationPolicy {
        INSTANT_CONFIRMATION, STAFF_APPROVAL_REQUIRED, DOCTOR_APPROVAL_REQUIRED
    }
}
