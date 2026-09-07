package com.clinic.platform.practitioner.domain;

import com.clinic.platform.clinic.domain.Clinic;
import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Section 7/section 43: deliberately separate from identity.User. A Practitioner is a
 * directory/clinical-directory record (name, specialties, which clinics they see patients at,
 * consultation fee) — it MAY optionally reference a User (userId) if the doctor also logs into
 * the doctor portal, but plenty of clinics maintain a doctor's schedule without ever giving
 * them portal credentials.
 */
@Entity
@Table(name = "practitioners")
@Getter
@Setter
public class Practitioner extends TenantScopedEntity {

    /** Nullable — set only if this practitioner also has portal login access. */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "credentials")
    private String credentials; // e.g. "MD, FPCP"

    @Column(name = "default_consultation_fee", precision = 10, scale = 2)
    private BigDecimal defaultConsultationFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PractitionerStatus status = PractitionerStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "practitioner_specialties",
            joinColumns = @JoinColumn(name = "practitioner_id"),
            inverseJoinColumns = @JoinColumn(name = "specialty_id")
    )
    private Set<Specialty> specialties = new HashSet<>();

    /** Which clinic branches this practitioner sees patients at — drives directory search (section 7). */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "practitioner_clinics",
            joinColumns = @JoinColumn(name = "practitioner_id"),
            inverseJoinColumns = @JoinColumn(name = "clinic_id")
    )
    private Set<Clinic> clinics = new HashSet<>();

    public enum PractitionerStatus {
        ACTIVE, ON_LEAVE, INACTIVE
    }
}
