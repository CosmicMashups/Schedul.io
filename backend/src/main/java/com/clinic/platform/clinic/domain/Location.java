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
 * Kept separate from Clinic even though a 1:1 Clinic:Location is the common case, because
 * some clinics run consultations from more than one physical site under the same branding/
 * scheduling policy (e.g. a main clinic plus a satellite site on certain days). Services and
 * Schedules can reference a Location directly when that distinction matters.
 */
@Entity
@Table(name = "locations")
@Getter
@Setter
public class Location extends TenantScopedEntity {

    @ManyToOne
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(nullable = false)
    private String name;

    @Column(name = "address_line", nullable = false)
    private String addressLine;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;
}
