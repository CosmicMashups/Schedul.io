package com.clinic.platform.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * A single grantable action, e.g. "appointment:create", "queue:call-next", "patient:read-clinical".
 * Not tenant-scoped: the permission catalog is platform-wide; which permissions a Role has is
 * what varies per tenant (see {@link Role}).
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code; // e.g. APPOINTMENT_CREATE

    @Column(nullable = false)
    private String description;
}
