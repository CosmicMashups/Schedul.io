package com.clinic.platform.notification.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Section 53: "Use a template system." body uses {{variable}} placeholders (section 53's own
 * example: "Your appointment with {{doctorName}} is confirmed for {{date}} at {{time}}.")
 * rendered by {@link com.clinic.platform.notification.service.TemplateRenderer}. One event
 * can have multiple templates — one per channel, and per section 53's "English / Filipino /
 * custom clinic templates" note, potentially more than one per (event, channel) once a
 * language column is actually load-bearing; language is captured now but every seeded
 * template uses "en" — multi-language selection logic is a follow-up once a clinic actually
 * asks for it.
 */
@Entity
@Table(name = "notification_templates", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "event_code", "channel", "language"}))
@Getter
@Setter
public class NotificationTemplate extends TenantScopedEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "event_code", nullable = false)
    private NotificationEvent eventCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String language = "en";

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(nullable = false)
    private boolean active = true;
}
