package com.clinic.platform.practitioner.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "specialties", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "code"}))
@Getter
@Setter
public class Specialty extends TenantScopedEntity {

    @Column(nullable = false)
    private String code; // e.g. INTERNAL_MEDICINE, DERMATOLOGY

    @Column(nullable = false)
    private String name; // display name
}
