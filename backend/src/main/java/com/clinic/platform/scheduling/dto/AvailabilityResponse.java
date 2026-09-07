package com.clinic.platform.scheduling.dto;

import java.util.List;
import java.util.UUID;

// Mirrors section 40's response shape: { practitionerId, serviceId, slots: [...] }
public record AvailabilityResponse(
        UUID practitionerId,
        UUID serviceId,
        List<SlotResponse> slots
) {
}
