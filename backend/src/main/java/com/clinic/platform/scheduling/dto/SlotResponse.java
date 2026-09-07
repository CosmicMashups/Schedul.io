package com.clinic.platform.scheduling.dto;

import com.clinic.platform.scheduling.domain.Slot;

import java.time.Instant;
import java.util.UUID;

public record SlotResponse(
        UUID slotId,
        UUID practitionerId,
        UUID clinicId,
        Instant start,
        Instant end,
        String status
) {
    public static SlotResponse from(Slot s) {
        return new SlotResponse(s.getId(), s.getPractitionerId(), s.getClinicId(), s.getStartAt(), s.getEndAt(), s.getStatus().name());
    }
}
