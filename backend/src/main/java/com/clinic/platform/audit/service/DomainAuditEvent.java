package com.clinic.platform.audit.service;

/**
 * Any module (appointment, scheduling, queue, patient) publishes this via Spring's
 * ApplicationEventPublisher rather than calling AuditService directly — keeps audit logging
 * decoupled and means a slow/failed audit write can never roll back the originating
 * transaction (listener runs AFTER_COMMIT, see AuditEventListener).
 */
public record DomainAuditEvent(
        String action,
        String entityType,
        String entityId,
        String beforeStateJson,
        String afterStateJson,
        String reason
) {
}
