package com.clinic.platform.queue.dto;

import com.clinic.platform.queue.domain.Queue;

import java.util.UUID;

public record QueueResponse(
        UUID id,
        UUID clinicId,
        String name,
        String type,
        String status
) {
    public static QueueResponse from(Queue q) {
        return new QueueResponse(q.getId(), q.getClinicId(), q.getName(), q.getType().name(), q.getStatus().name());
    }
}
