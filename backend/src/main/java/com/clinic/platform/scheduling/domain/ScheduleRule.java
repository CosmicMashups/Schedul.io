package com.clinic.platform.scheduling.domain;

import com.clinic.platform.clinic.domain.Clinic;
import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Section 10: recurring weekly availability, e.g. "Dr. Santos, Monday, 08:00-12:00,
 * slotDuration=30min". This is a RULE, not a slot — {@link com.clinic.platform.scheduling.service.SlotGenerationService}
 * expands rules (minus exceptions) into concrete {@link Slot} rows on a rolling horizon
 * (section 11), it does not pre-generate years of slots.
 */
@Entity
@Table(name = "schedule_rules")
@Getter
@Setter
public class ScheduleRule extends TenantScopedEntity {

    @Column(name = "practitioner_id", nullable = false)
    private UUID practitionerId;

    @ManyToOne
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Nullable = open-ended (still in effect indefinitely). */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "slot_duration_minutes", nullable = false)
    private int slotDurationMinutes;

    @Column(name = "buffer_minutes", nullable = false)
    private int bufferMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleStatus status = RuleStatus.ACTIVE;

    public enum RuleStatus {
        ACTIVE, INACTIVE
    }
}
