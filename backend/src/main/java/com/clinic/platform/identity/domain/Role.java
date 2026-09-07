package com.clinic.platform.identity.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Section 43 roles (SUPER_ADMIN, TENANT_ADMIN, CLINIC_ADMIN, RECEPTIONIST, NURSE,
 * TRIAGE_STAFF, DOCTOR, SCHEDULER, PATIENT) are seeded per tenant as system roles, but
 * tenant admins can create additional custom roles — hence Role is tenant-scoped rather
 * than a hardcoded enum.
 */
@Entity
@Table(name = "roles", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = {"tenant_id", "code"}))
@Getter
@Setter
public class Role extends TenantScopedEntity {

    @Column(nullable = false)
    private String code; // e.g. RECEPTIONIST

    @Column(nullable = false)
    private String name;

    /** System roles are seeded per tenant and cannot be deleted (only custom roles can). */
    @Column(name = "is_system_role", nullable = false)
    private boolean systemRole = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
