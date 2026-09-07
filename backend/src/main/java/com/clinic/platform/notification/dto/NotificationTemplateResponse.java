package com.clinic.platform.notification.dto;

import com.clinic.platform.notification.domain.NotificationTemplate;

import java.util.UUID;

public record NotificationTemplateResponse(
        UUID id, String eventCode, String channel, String language, String body, boolean active
) {
    public static NotificationTemplateResponse from(NotificationTemplate t) {
        return new NotificationTemplateResponse(t.getId(), t.getEventCode().name(), t.getChannel().name(),
                t.getLanguage(), t.getBody(), t.isActive());
    }
}
