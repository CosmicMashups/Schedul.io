package com.clinic.platform.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

/**
 * Used by every module's service layer to build the beforeStateJson/afterStateJson fields of
 * a {@link com.clinic.platform.audit.service.DomainAuditEvent}. Centralized so a bad
 * serialization (e.g. a lazy-loaded association) fails the same way everywhere instead of each
 * module hand-rolling its own snapshot string.
 */
@Component
public class AuditSnapshot {

    private final ObjectMapper mapper;

    public AuditSnapshot() {
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /** Never throws — a snapshot failure must not block the business operation it's auditing. */
    public String of(Object entity) {
        if (entity == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(entity);
        } catch (Exception ex) {
            return "{\"_snapshotError\":\"" + ex.getClass().getSimpleName() + "\"}";
        }
    }
}
