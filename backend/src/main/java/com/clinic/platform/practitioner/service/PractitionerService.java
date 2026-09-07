package com.clinic.platform.practitioner.service;

import com.clinic.platform.audit.service.DomainAuditEvent;
import com.clinic.platform.clinic.domain.Clinic;
import com.clinic.platform.clinic.repository.ClinicRepository;
import com.clinic.platform.common.exception.ApiException;
import com.clinic.platform.common.util.AuditSnapshot;
import com.clinic.platform.identity.repository.UserRepository;
import com.clinic.platform.practitioner.domain.Practitioner;
import com.clinic.platform.practitioner.domain.Specialty;
import com.clinic.platform.practitioner.dto.PractitionerRequest;
import com.clinic.platform.practitioner.dto.PractitionerResponse;
import com.clinic.platform.practitioner.repository.PractitionerRepository;
import com.clinic.platform.practitioner.repository.SpecialtyRepository;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PractitionerService {

    private final PractitionerRepository practitionerRepository;
    private final SpecialtyRepository specialtyRepository;
    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;
    private final AuditSnapshot auditSnapshot;

    public PractitionerService(PractitionerRepository practitionerRepository, SpecialtyRepository specialtyRepository,
                                ClinicRepository clinicRepository, UserRepository userRepository,
                                ApplicationEventPublisher events, AuditSnapshot auditSnapshot) {
        this.practitionerRepository = practitionerRepository;
        this.specialtyRepository = specialtyRepository;
        this.clinicRepository = clinicRepository;
        this.userRepository = userRepository;
        this.events = events;
        this.auditSnapshot = auditSnapshot;
    }

    @Transactional
    public PractitionerResponse create(PractitionerRequest request) {
        Practitioner practitioner = new Practitioner();
        applyRequest(practitioner, request);
        Practitioner saved = practitionerRepository.save(practitioner);

        events.publishEvent(new DomainAuditEvent(
                "PRACTITIONER_CREATE", "Practitioner", saved.getId().toString(), null, auditSnapshot.of(saved), null));

        return PractitionerResponse.from(saved);
    }

    @Transactional
    public PractitionerResponse update(UUID id, PractitionerRequest request) {
        Practitioner practitioner = getOwnedOrThrow(id);
        String before = auditSnapshot.of(practitioner);
        applyRequest(practitioner, request);
        Practitioner saved = practitionerRepository.save(practitioner);

        events.publishEvent(new DomainAuditEvent(
                "PRACTITIONER_UPDATE", "Practitioner", saved.getId().toString(), before, auditSnapshot.of(saved), null));

        return PractitionerResponse.from(saved);
    }

    @Transactional
    public void setOnLeave(UUID id, boolean onLeave) {
        Practitioner practitioner = getOwnedOrThrow(id);
        String before = auditSnapshot.of(practitioner);
        practitioner.setStatus(onLeave ? Practitioner.PractitionerStatus.ON_LEAVE : Practitioner.PractitionerStatus.ACTIVE);
        practitionerRepository.save(practitioner);

        // NOTE: does not yet cascade to affected appointments — that's section 27's
        // "doctor absence" workflow, which depends on the Appointment module (Milestone 4).
        events.publishEvent(new DomainAuditEvent(
                "PRACTITIONER_STATUS_CHANGE", "Practitioner", id.toString(), before, auditSnapshot.of(practitioner), null));
    }

    public PractitionerResponse get(UUID id) {
        return PractitionerResponse.from(getOwnedOrThrow(id));
    }

    /**
     * New for the Doctor Portal: resolves the Practitioner record linked to the calling
     * User's account (Practitioner.userId), the same pattern as PatientService.getMe(). A
     * doctor logging into the portal needs their own practitionerId to scope "my schedule" /
     * "my queue" — there was previously no way to get that without already knowing it.
     */
    public PractitionerResponse getMe() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = com.clinic.platform.security.CurrentUser.require();
        Practitioner practitioner = practitionerRepository.findByTenantIdAndUserIdAndDeletedFalse(tenantId, userId)
                .orElseThrow(() -> ApiException.notFound("PRACTITIONER_RECORD_NOT_FOUND",
                        "No practitioner record is linked to this account."));
        return PractitionerResponse.from(practitioner);
    }

    /** Section 7 doctor directory search. */
    public List<PractitionerResponse> search(String specialtyCode, UUID clinicId, String nameQuery) {
        UUID tenantId = TenantContext.requireTenantId();
        return practitionerRepository.search(tenantId, specialtyCode, clinicId, nameQuery).stream()
                .map(PractitionerResponse::from)
                .toList();
    }

    /** New for the Doctor Portal "invite" flow: sets/replaces Practitioner.userId without requiring the full PractitionerRequest payload (specialties/clinics are unrelated to this action). */
    @Transactional
    public PractitionerResponse linkUser(UUID practitionerId, UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();
        Practitioner practitioner = getOwnedOrThrow(practitionerId);

        userRepository.findById(userId)
                .filter(u -> u.getTenantId().equals(tenantId))
                .orElseThrow(() -> ApiException.badRequest("INVALID_USER", "User does not belong to this tenant."));

        String before = auditSnapshot.of(practitioner);
        practitioner.setUserId(userId);
        Practitioner saved = practitionerRepository.save(practitioner);

        events.publishEvent(new DomainAuditEvent(
                "PRACTITIONER_LINK_USER", "Practitioner", practitionerId.toString(), before, auditSnapshot.of(saved), null));

        return PractitionerResponse.from(saved);
    }

    private Practitioner getOwnedOrThrow(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return practitionerRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("PRACTITIONER_NOT_FOUND", "Practitioner not found."));
    }

    private void applyRequest(Practitioner practitioner, PractitionerRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        practitioner.setFirstName(request.firstName());
        practitioner.setLastName(request.lastName());
        practitioner.setCredentials(request.credentials());
        practitioner.setDefaultConsultationFee(request.defaultConsultationFee());

        Set<Specialty> specialties = new HashSet<>(specialtyRepository.findAllById(request.specialtyIds()));
        specialties.removeIf(s -> !s.getTenantId().equals(tenantId));
        if (specialties.size() != request.specialtyIds().size()) {
            throw ApiException.badRequest("INVALID_SPECIALTY", "One or more specialties do not belong to this tenant.");
        }
        practitioner.setSpecialties(specialties);

        Set<Clinic> clinics = new HashSet<>(clinicRepository.findAllById(request.clinicIds()));
        clinics.removeIf(c -> !c.getTenantId().equals(tenantId));
        if (clinics.size() != request.clinicIds().size()) {
            throw ApiException.badRequest("INVALID_CLINIC", "One or more clinics do not belong to this tenant.");
        }
        practitioner.setClinics(clinics);

        if (request.userId() != null) {
            userRepository.findById(request.userId())
                    .filter(u -> u.getTenantId().equals(tenantId))
                    .orElseThrow(() -> ApiException.badRequest("INVALID_USER", "User does not belong to this tenant."));
            practitioner.setUserId(request.userId());
        }
    }
}
