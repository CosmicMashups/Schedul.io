package com.clinic.platform.notification.service;

import com.clinic.platform.notification.domain.Notification;
import com.clinic.platform.notification.domain.NotificationChannel;
import com.clinic.platform.notification.domain.NotificationEvent;
import com.clinic.platform.notification.domain.NotificationTemplate;
import com.clinic.platform.notification.repository.NotificationRepository;
import com.clinic.platform.notification.repository.NotificationTemplateRepository;
import com.clinic.platform.patient.domain.Patient;
import com.clinic.platform.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Section 42: "Don't make the appointment transaction directly call every external service."
 * This is the single place that turns "something happened" into "a message went out" - every
 * *NotificationListener (appointment, queue) calls this rather than rendering/sending
 * anything itself.
 *
 * Section 54's integration boundary: {@link #send} is a stub that logs and records SENT.
 * Swapping in a real SMS gateway or email provider means changing that one method - the
 * event listeners, template system, and Notification audit trail all stay exactly as they
 * are. Building the real gateway integration now, with no gateway account/credentials to
 * test against, would just be unverifiable code.
 *
 * REQUIRES_NEW: dispatch runs in its own transaction, separate from whatever
 * @TransactionalEventListener(AFTER_COMMIT) invoked it - a notification failure (bad
 * template, DB hiccup writing the Notification row) must never be able to affect a business
 * transaction that has, by definition, already committed by the time this runs.
 */
@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationTemplateRepository templateRepository;
    private final NotificationRepository notificationRepository;
    private final TemplateRenderer renderer;

    public NotificationDispatchService(NotificationTemplateRepository templateRepository,
                                        NotificationRepository notificationRepository, TemplateRenderer renderer) {
        this.templateRepository = templateRepository;
        this.notificationRepository = notificationRepository;
        this.renderer = renderer;
    }

    /**
     * Fans out to every active template configured for this event (one per channel, per
     * section 53). A tenant with no template configured for an event simply gets no
     * notification for it - a silent no-op by design until a tenant actually configures
     * templates, rather than a recorded failure for something that was never expected to fire.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(NotificationEvent event, Patient patient, String relatedEntityType, String relatedEntityId,
                          Map<String, String> variables) {
        UUID tenantId = TenantContext.requireTenantId();
        List<NotificationTemplate> templates = templateRepository
                .findByTenantIdAndEventCodeAndActiveTrueAndDeletedFalse(tenantId, event);

        for (NotificationTemplate template : templates) {
            String contact = resolveContact(patient, template.getChannel());
            Notification notification = new Notification();
            notification.setEventCode(event);
            notification.setChannel(template.getChannel());
            notification.setRecipientPatientId(patient.getId());
            notification.setRecipientContact(contact);
            notification.setRelatedEntityType(relatedEntityType);
            notification.setRelatedEntityId(relatedEntityId);

            if (contact == null) {
                notification.setStatus(Notification.DeliveryStatus.SKIPPED_NO_CONTACT);
                notification.setRenderedBody("");
                notificationRepository.save(notification);
                continue;
            }

            String rendered = renderer.render(template.getBody(), variables);
            notification.setRenderedBody(rendered);

            try {
                send(template.getChannel(), contact, rendered);
                notification.setStatus(Notification.DeliveryStatus.SENT);
                notification.setSentAt(Instant.now());
            } catch (Exception ex) {
                notification.setStatus(Notification.DeliveryStatus.FAILED);
                notification.setErrorMessage(ex.getMessage());
                log.warn("Notification dispatch failed for patient {} event {}: {}", patient.getId(), event, ex.getMessage());
            }
            notificationRepository.save(notification);
        }
    }

    /** Section 54 integration stub - replace with a real SMS/email gateway call. */
    private void send(NotificationChannel channel, String contact, String body) {
        log.info("[STUB NOTIFICATION] channel={} to={} body=\"{}\"", channel, contact, body);
    }

    private String resolveContact(Patient patient, NotificationChannel channel) {
        return switch (channel) {
            case SMS -> patient.getMobileNumber();
            case EMAIL -> patient.getEmail();
            case PUSH, IN_APP -> null; // no device/session registry exists yet to resolve these against
        };
    }
}
