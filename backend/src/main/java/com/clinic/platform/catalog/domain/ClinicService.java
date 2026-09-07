package com.clinic.platform.catalog.domain;

import com.clinic.platform.clinic.domain.Clinic;
import com.clinic.platform.common.entity.TenantScopedEntity;
import com.clinic.platform.practitioner.domain.Specialty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Section 8: an appointment books a Service, not just a doctor. durationMinutes and
 * bufferMinutes feed slot generation in the Scheduling Engine (Milestone 3); allowedSpecialty
 * constrains which practitioners can be booked for it in the availability query.
 */
@Entity
@Table(name = "services")
@Getter
@Setter
public class ClinicService extends TenantScopedEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "buffer_minutes", nullable = false)
    private int bufferMinutes = 0;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_mode", nullable = false)
    private ConsultationMode consultationMode = ConsultationMode.IN_PERSON;

    @ManyToOne
    @JoinColumn(name = "allowed_specialty_id")
    private Specialty allowedSpecialty; // nullable: some services (e.g. "Medical Certificate") aren't specialty-restricted

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceStatus status = ServiceStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "service_clinics",
            joinColumns = @JoinColumn(name = "service_id"),
            inverseJoinColumns = @JoinColumn(name = "clinic_id")
    )
    private Set<Clinic> clinics = new HashSet<>();

    public enum ConsultationMode {
        IN_PERSON, TELECONSULT, EITHER
    }

    public enum ServiceStatus {
        ACTIVE, INACTIVE
    }
}
