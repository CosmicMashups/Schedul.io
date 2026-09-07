package com.clinic.platform.clinic.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Section 36: a Tenant (organization) owns one or more Clinics (branches). Every other
 * operational table (Practitioner, Service, Schedule, Appointment...) is scoped to a Clinic
 * within the tenant, not just the tenant, once multi-branch support matters — clinic_id is
 * carried alongside tenant_id from Milestone 3 onward wherever a resource is branch-specific.
 */
@Entity
@Table(name = "clinics")
@Getter
@Setter
public class Clinic extends TenantScopedEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "address_line", nullable = false)
    private String addressLine;

    @Column(name = "city")
    private String city;

    @Column(name = "province")
    private String province;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "timezone", nullable = false)
    private String timezone = "Asia/Manila";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClinicStatus status = ClinicStatus.ACTIVE;

    public enum ClinicStatus {
        ACTIVE, INACTIVE
    }
}
