package com.clinic.platform.scheduling.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Section 10: LEAVE / HOLIDAY / MEETING / CONFERENCE / EMERGENCY / MANUAL_BLOCK. Applied by
 * {@link com.clinic.platform.scheduling.service.SlotGenerationService} at generation time
 * (skips overlapping slots) AND checked again at hold/booking time in case an exception is
 * added after slots already exist for that date (section 27: doctor absence handling).
 */
@Entity
@Table(name = "schedule_exceptions")
@Getter
@Setter
public class ScheduleException extends TenantScopedEntity {

    @Column(name = "practitioner_id", nullable = false)
    private UUID practitionerId;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    /** Null start/end = the whole day is blocked. */
    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExceptionType type;

    @Column(name = "reason")
    private String reason;

    public enum ExceptionType {
        LEAVE, HOLIDAY, MEETING, CONFERENCE, EMERGENCY, MANUAL_BLOCK
    }
}
