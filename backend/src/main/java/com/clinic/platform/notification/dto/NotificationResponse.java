package com.clinic.platform.notification.dto;

import com.clinic.platform.notification.domain.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id, String eventCode, String channel, String recipientContact, String renderedBody,
        String status, Instant sentAt, String errorMessage
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getEventCode().name(), n.getChannel().name(),
                n.getRecipientContact(), n.getRenderedBody(), n.getStatus().name(), n.getSentAt(), n.getErrorMessage());
    }
}
