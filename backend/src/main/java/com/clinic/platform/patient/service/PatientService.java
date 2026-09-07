package com.clinic.platform.patient.service;

import com.clinic.platform.audit.service.DomainAuditEvent;
import com.clinic.platform.common.exception.ApiException;
import com.clinic.platform.common.util.AuditSnapshot;
import com.clinic.platform.patient.domain.Patient;
import com.clinic.platform.patient.domain.PatientConsent;
import com.clinic.platform.patient.dto.PatientRegistrationRequest;
import com.clinic.platform.patient.dto.PatientResponse;
import com.clinic.platform.patient.repository.PatientConsentRepository;
import com.clinic.platform.patient.repository.PatientRepository;
import com.clinic.platform.security.CurrentUser;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientConsentRepository consentRepository;
    private final ApplicationEventPublisher events;
    private final AuditSnapshot auditSnapshot;

    public PatientService(PatientRepository patientRepository, PatientConsentRepository consentRepository,
                           ApplicationEventPublisher events, AuditSnapshot auditSnapshot) {
        this.patientRepository = patientRepository;
        this.consentRepository = consentRepository;
        this.events = events;
        this.auditSnapshot = auditSnapshot;
    }

    /**
     * Section 6: consent capture is part of the registration transaction, not an optional
     * follow-up step — dataProcessingConsentGranted is validated at the DTO level
     * (@AssertTrue) so registration cannot complete without it.
     */
    @Transactional
    public PatientResponse register(PatientRegistrationRequest request) {
        Patient patient = new Patient();
        patient.setFirstName(request.firstName());
        patient.setMiddleName(request.middleName());
        patient.setLastName(request.lastName());
        patient.setSuffix(request.suffix());
        patient.setBirthDate(request.birthDate());
        patient.setSex(request.sex());
        patient.setMobileNumber(request.mobileNumber());
        patient.setEmail(request.email());
        patient.setAddressLine(request.addressLine());
        patient.setEmergencyContactName(request.emergencyContactName());
        patient.setEmergencyContactNumber(request.emergencyContactNumber());
        if (request.preferredContactMethod() != null) {
            patient.setPreferredContactMethod(request.preferredContactMethod());
        }
        if (request.registrationSource() != null) {
            patient.setRegistrationSource(request.registrationSource());
        }

        // Closes a gap flagged during the frontend build: only link this Patient to the
        // calling User when they registered THEMSELVES (SELF_REGISTER authority) — a staff
        // member registering a walk-in or front-desk patient must never have their own
        // account linked to that patient's record. This is what makes GET /patients/me work
        // for the patient portal without also mislinking staff-created records.
        // NOTE: the seeded dev TENANT_ADMIN role is granted every permission (see V2/V3
        // migrations) for local-testing convenience, including both PATIENT_WRITE and
        // SELF_REGISTER — so this check cannot distinguish "admin testing the staff flow"
        // from "admin testing the patient flow" using that account. A real deployment's
        // roles won't overlap this way (a patient-portal account only ever has
        // SELF_REGISTER); flagged here so the ambiguity isn't mistaken for a logic bug.
        boolean isSelfRegistration = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> "SELF_REGISTER".equals(a.getAuthority()));
        if (isSelfRegistration) {
            CurrentUser.id().ifPresent(patient::setUserId);
        }

        Patient saved = patientRepository.save(patient);

        PatientConsent consent = new PatientConsent();
        consent.setPatient(saved);
        consent.setConsentType(PatientConsent.ConsentType.DATA_PROCESSING);
        consent.setPrivacyNoticeVersion(
                request.privacyNoticeVersion() != null ? request.privacyNoticeVersion() : "v1");
        consent.setGranted(true);
        consent.setGrantedAt(Instant.now());
        consentRepository.save(consent);

        events.publishEvent(new DomainAuditEvent(
                "PATIENT_REGISTER", "Patient", saved.getId().toString(), null,
                auditSnapshot.of(saved), "source=" + saved.getRegistrationSource()));

        return PatientResponse.from(saved);
    }

    @Transactional
    public void withdrawConsent(UUID patientId, PatientConsent.ConsentType type) {
        UUID tenantId = TenantContext.requireTenantId();
        List<PatientConsent> history = consentRepository.findByTenantIdAndPatientIdOrderByCreatedAtDesc(tenantId, patientId);
        PatientConsent latest = history.stream()
                .filter(c -> c.getConsentType() == type && c.isGranted())
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("CONSENT_NOT_FOUND", "No active consent of this type to withdraw."));

        String before = auditSnapshot.of(latest);
        latest.setGranted(false);
        latest.setWithdrawnAt(Instant.now());
        consentRepository.save(latest);

        events.publishEvent(new DomainAuditEvent(
                "PATIENT_CONSENT_WITHDRAW", "PatientConsent", latest.getId().toString(), before, auditSnapshot.of(latest), null));
    }

    public PatientResponse get(UUID id) {
        return PatientResponse.from(getOwnedOrThrow(id));
    }

    /** Closes the "no way to resolve a returning patient's record" gap flagged in the patient portal README. */
    public PatientResponse getMe() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = CurrentUser.require();
        Patient patient = patientRepository.findByTenantIdAndUserIdAndDeletedFalse(tenantId, userId)
                .orElseThrow(() -> ApiException.notFound("PATIENT_RECORD_NOT_FOUND",
                        "No patient record is linked to this account yet."));
        return PatientResponse.from(patient);
    }

    /** Staff search by name, mobile, or email (section 32-ish, used by check-in/phone booking). */
    public List<PatientResponse> search(String query) {
        UUID tenantId = TenantContext.requireTenantId();
        return patientRepository.search(tenantId, query).stream()
                .map(PatientResponse::from)
                .toList();
    }

    private Patient getOwnedOrThrow(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return patientRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("PATIENT_NOT_FOUND", "Patient not found."));
    }
}
