package com.clinic.platform.audit.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;

/**
 * Append-only by convention: no service in the codebase should ever UPDATE or DELETE a row
 * here (enforce with a DB trigger/permissions in production — see V1 migration comment).
 * Kept deliberately generic (actorId/entityType/entityId + JSON before/after) rather than one
 * table per domain event, since audit requirements span every module (appointment, schedule,
 * patient, queue) and a single queryable timeline is more useful for compliance review.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog extends TenantScopedEntity {

    @Column(name = "actor_user_id")
    private String actorUserId; // nullable: system-initiated events (e.g. auto-expired hold)

    @Column(name = "actor_display_name")
    private String actorDisplayName;

    @Column(name = "action", nullable = false)
    private String action; // e.g. RESCHEDULE_APPOINTMENT, CHECK_IN, QUEUE_REORDER

    @Column(name = "entity_type", nullable = false)
    private String entityType; // e.g. Appointment, QueueTicket

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state")
    private String beforeState; // JSON snapshot, nullable for CREATE actions

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state")
    private String afterState; // JSON snapshot, nullable for DELETE actions

    @Column(name = "reason")
    private String reason;

    @Column(name = "ip_address")
    private String ipAddress;
}
