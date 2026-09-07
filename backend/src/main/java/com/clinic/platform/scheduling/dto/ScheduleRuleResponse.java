package com.clinic.platform.scheduling.dto;

import com.clinic.platform.scheduling.domain.ScheduleRule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleRuleResponse(
        UUID id,
        UUID practitionerId,
        UUID clinicId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        int slotDurationMinutes,
        int bufferMinutes,
        String status
) {
    public static ScheduleRuleResponse from(ScheduleRule r) {
        return new ScheduleRuleResponse(
                r.getId(), r.getPractitionerId(), r.getClinic().getId(), r.getDayOfWeek(),
                r.getStartTime(), r.getEndTime(), r.getEffectiveFrom(), r.getEffectiveTo(),
                r.getSlotDurationMinutes(), r.getBufferMinutes(), r.getStatus().name()
        );
    }
}
