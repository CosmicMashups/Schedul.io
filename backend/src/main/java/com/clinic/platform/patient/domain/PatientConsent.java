package com.clinic.platform.patient.domain;

import com.clinic.platform.common.entity.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Section 6: Philippine Data Privacy Act — health information is sensitive personal
 * information, so consent is tracked as its own record (with a version of the notice/terms
 * the patient agreed to) rather than a single boolean on Patient. A patient can have multiple
 * consent records over time (re-consent after a privacy notice update, consent withdrawal).
 */
@Entity
@Table(name = "patient_consents")
@Getter
@Setter
public class PatientConsent extends TenantScopedEntity {

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    private ConsentType consentType;

    @Column(name = "privacy_notice_version", nullable = false)
    private String privacyNoticeVersion;

    @Column(name = "granted", nullable = false)
    private boolean granted;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    public enum ConsentType {
        DATA_PROCESSING, MARKETING_COMMUNICATIONS, TELECONSULT_RECORDING
    }
}
