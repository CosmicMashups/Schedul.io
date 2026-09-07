package com.clinic.platform.notification.dto;

import com.clinic.platform.notification.domain.NotificationChannel;
import com.clinic.platform.notification.domain.NotificationEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationTemplateRequest(
        @NotNull NotificationEvent eventCode,
        @NotNull NotificationChannel channel,
        String language,
        @NotBlank String body
) {
}
