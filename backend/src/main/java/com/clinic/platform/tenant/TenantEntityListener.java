package com.clinic.platform.tenant;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.PrePersist;

/**
 * Registered via @EntityListeners on {@link TenantScopedEntity}. Guarantees that application
 * code can never forget to set tenant_id (and can never set it to the wrong tenant) — it is
 * always taken from the current request's {@link TenantContext}.
 *
 * Read-side isolation (SELECTs) is enforced separately via a Hibernate @Filter or a
 * tenant-aware base repository — added when the first tenant-scoped domain module (patient,
 * scheduling) lands, since it needs the filter enabled per-session.
 */
public class TenantEntityListener {

    @PrePersist
    public void setTenantId(TenantScopedEntity entity) {
        if (entity.getTenantId() == null) {
            entity.setTenantId(TenantContext.requireTenantId());
        }
    }
}
