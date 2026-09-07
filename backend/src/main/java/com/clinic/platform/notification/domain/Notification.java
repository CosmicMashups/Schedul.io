package com.clinic.platform.notification.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Section 42's event-driven principle in concrete form: this row is the record of "we tried
 * to notify someone", independent of whether the underlying gateway call actually succeeded.
 * {@link #status} being FAILED never rolls back or affects the business transaction that
 * triggered it — see {@link com.clinic.platform.notification.service.NotificationDispatchService}.
 *
 * No real SMS/email gateway is integrated in this milestone (section 54's integration
 * boundary — SMS gateway, email provider — is a Phase 2/3 concern per the original
 * architecture review's phasing). {@link #status} of SENT here means "the dispatch service
 * successfully rendered and logged the notification", not "a carrier confirmed delivery".
 * Swapping in a real gateway means changing NotificationDispatchService's send step only —
 * this table and everything upstream of it stays the same.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification extends TenantScopedEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "event_code", nullable = false)
    private NotificationEvent eventCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(name = "recipient_patient_id", nullable = false)
    private UUID recipientPatientId;

    @Column(name = "recipient_contact")
    private String recipientContact; // snapshot of the mobile/email used at send time

    @Column(name = "rendered_body", nullable = false, length = 1000)
    private String renderedBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private String relatedEntityId;

    public enum DeliveryStatus {
        PENDING, SENT, FAILED, SKIPPED_NO_CONTACT, SKIPPED_NO_TEMPLATE
    }
}
