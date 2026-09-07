package com.clinic.platform.queue.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Section 21: "I would not make one global queue." A clinic has multiple named queues
 * (REGISTRATION, TRIAGE, DOCTOR, LAB, PROCEDURE); a patient's QueueTicket moves between them
 * as they progress (section 21's diagram). Milestone 7 wires DOCTOR-queue check-in flow
 * end-to-end; REGISTRATION/TRIAGE/LAB/PROCEDURE queues can be created via this same entity
 * once those workflows are needed, without a schema change.
 */
@Entity
@Table(name = "queues")
@Getter
@Setter
public class Queue extends TenantScopedEntity {

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueStatus status = QueueStatus.OPEN;

    public enum QueueType {
        REGISTRATION, TRIAGE, DOCTOR, LABORATORY, PROCEDURE
    }

    public enum QueueStatus {
        OPEN, CLOSED
    }
}
