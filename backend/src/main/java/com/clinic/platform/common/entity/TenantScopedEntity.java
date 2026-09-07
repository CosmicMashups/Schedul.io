package com.clinic.platform.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import com.clinic.platform.tenant.TenantEntityListener;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Every tenant-owned table (patients, appointments, schedules, etc.) extends this.
 * tenant_id is mandatory and is stamped automatically from the request's TenantContext
 * via {@link com.clinic.platform.tenant.TenantEntityListener} at persist time — domain
 * code should never set it manually.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class, TenantEntityListener.class})
public abstract class TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    /** Soft delete — enterprise/audit systems should not hard-delete records. */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}
