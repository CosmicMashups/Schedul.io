package com.clinic.platform.scheduling.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Sections 10-13. Mirrors FHIR's Slot states (free/busy/busy-tentative/entered-in-error) via
 * {@link SlotStatus}. HELD is the busy-tentative equivalent — a short-lived reservation
 * (section 12) created by {@link com.clinic.platform.scheduling.service.SlotHoldService}
 * while a patient completes the booking form; it auto-expires back to FREE if not converted
 * to BOOKED before {@code heldUntil}.
 *
 * Concurrency (section 13): booking a slot requires a pessimistic row lock
 * (SELECT ... FOR UPDATE) so two simultaneous requests for the same slot cannot both succeed —
 * see {@link com.clinic.platform.scheduling.repository.SlotRepository#lockForUpdate}.
 * {@code @Version} is kept as a defense-in-depth optimistic check for any code path that
 * doesn't go through the locking repository method.
 */
@Entity
@Table(name = "slots", uniqueConstraints = @UniqueConstraint(columnNames = {"practitioner_id", "start_at"}))
@Getter
@Setter
public class Slot extends TenantScopedEntity {

    @Column(name = "schedule_rule_id")
    private UUID scheduleRuleId; // nullable: manually created/overbooked slots aren't rule-generated

    @Column(name = "practitioner_id", nullable = false)
    private UUID practitionerId;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus status = SlotStatus.FREE;

    @Column(name = "held_by_user_id")
    private UUID heldByUserId;

    @Column(name = "held_until")
    private Instant heldUntil;

    @Column(name = "appointment_id")
    private UUID appointmentId; // set once booked — Appointment itself lands in Milestone 4

    @Column(name = "overbooked", nullable = false)
    private boolean overbooked = false;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public enum SlotStatus {
        FREE, HELD, BOOKED, BLOCKED, ENTERED_IN_ERROR
    }
}
