package com.clinic.platform.clinic.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Not used by scheduling/queue logic yet (Milestones 3/6) but modeled now since it's pure
 * master data — a receptionist directing a patient to "Room 4" needs Room to exist as a
 * referenceable entity, not a free-text string scattered across later tables.
 */
@Entity
@Table(name = "rooms")
@Getter
@Setter
public class Room extends TenantScopedEntity {

    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false)
    private String name; // e.g. "Room 4", "Dental Bay 2"

    @Column(name = "room_type")
    private String roomType; // e.g. CONSULTATION, PROCEDURE, LAB
}
