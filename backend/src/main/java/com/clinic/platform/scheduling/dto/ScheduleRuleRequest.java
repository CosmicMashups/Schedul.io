package com.clinic.platform.scheduling.dto;

import jakarta.validation.constraints.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleRuleRequest(
        @NotNull UUID practitionerId,
        @NotNull UUID clinicId,
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        @Min(5) int slotDurationMinutes,
        @Min(0) int bufferMinutes
) {
}
