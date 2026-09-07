package com.clinic.platform.audit.service;

import com.clinic.platform.audit.domain.AuditLog;
import com.clinic.platform.audit.domain.AuditLogRepository;
import com.clinic.platform.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditLogRepository auditLogRepository;

    public AuditEventListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * AFTER_COMMIT: the audit record is only written once the business transaction has actually
     * succeeded, and a failure here (e.g. DB hiccup) can never roll back a completed appointment
     * booking or check-in — it only gets logged, per the event-driven principle in section 42.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainAuditEvent(DomainAuditEvent event) {
        try {
            AuditLog entry = new AuditLog();
            entry.setTenantId(TenantContext.getTenantId());
            entry.setActorUserId(currentActorId());
            entry.setActorDisplayName(currentActorName());
            entry.setAction(event.action());
            entry.setEntityType(event.entityType());
            entry.setEntityId(event.entityId());
            entry.setBeforeState(event.beforeStateJson());
            entry.setAfterState(event.afterStateJson());
            entry.setReason(event.reason());
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Failed to persist audit event for {} {}: {}", event.entityType(), event.entityId(), ex.getMessage(), ex);
        }
    }

    private String currentActorId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private String currentActorName() {
        return currentActorId();
    }
}
