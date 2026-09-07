package com.clinic.platform.patient.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Section 6: registration fields only — no clinical documentation here by design (this
 * product's identity stays "Appointment → Check-in → Queue → Visit flow", not an EHR; see
 * section 66). Sex is a required registration field for care purposes (distinct from gender
 * identity, which this system does not collect). userId is nullable for the same reason as
 * Practitioner.userId: many patients are registered by staff/walk-in and never get portal
 * credentials.
 */
@Entity
@Table(name = "patients")
@Getter
@Setter
public class Patient extends TenantScopedEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "suffix")
    private String suffix;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sex sex;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "address_line")
    private String addressLine;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_number")
    private String emergencyContactNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_contact_method", nullable = false)
    private ContactMethod preferredContactMethod = ContactMethod.SMS;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_source", nullable = false)
    private RegistrationSource registrationSource = RegistrationSource.FRONT_DESK;

    public enum Sex {
        MALE, FEMALE
    }

    public enum ContactMethod {
        SMS, EMAIL, PHONE_CALL
    }

    /** Mirrors the "Appointment Source" concept from section 28, applied to how the patient record itself originated. */
    public enum RegistrationSource {
        ONLINE, PHONE, FRONT_DESK, WALK_IN, REFERRAL, STAFF
    }
}
