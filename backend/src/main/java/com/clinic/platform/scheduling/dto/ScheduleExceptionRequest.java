package com.clinic.platform.scheduling.dto;

import com.clinic.platform.scheduling.domain.ScheduleException;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleExceptionRequest(
        @NotNull UUID practitionerId,
        @NotNull LocalDate exceptionDate,
        LocalTime startTime,   // null = whole day blocked
        LocalTime endTime,
        @NotNull ScheduleException.ExceptionType type,
        String reason
) {
}
