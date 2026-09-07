package com.clinic.platform.clinic.service;

import com.clinic.platform.audit.service.DomainAuditEvent;
import com.clinic.platform.clinic.domain.Clinic;
import com.clinic.platform.clinic.dto.ClinicRequest;
import com.clinic.platform.clinic.dto.ClinicResponse;
import com.clinic.platform.clinic.repository.ClinicRepository;
import com.clinic.platform.common.exception.ApiException;
import com.clinic.platform.common.util.AuditSnapshot;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClinicService {

    private final ClinicRepository clinicRepository;
    private final ApplicationEventPublisher events;
    private final AuditSnapshot auditSnapshot;

    public ClinicService(ClinicRepository clinicRepository, ApplicationEventPublisher events, AuditSnapshot auditSnapshot) {
        this.clinicRepository = clinicRepository;
        this.events = events;
        this.auditSnapshot = auditSnapshot;
    }

    @Transactional
    public ClinicResponse create(ClinicRequest request) {
        Clinic clinic = new Clinic();
        applyRequest(clinic, request);
        Clinic saved = clinicRepository.save(clinic);

        events.publishEvent(new DomainAuditEvent(
                "CLINIC_CREATE", "Clinic", saved.getId().toString(), null, auditSnapshot.of(saved), null));

        return ClinicResponse.from(saved);
    }

    @Transactional
    public ClinicResponse update(UUID id, ClinicRequest request) {
        Clinic clinic = getOwnedOrThrow(id);
        String before = auditSnapshot.of(clinic);
        applyRequest(clinic, request);
        Clinic saved = clinicRepository.save(clinic);

        events.publishEvent(new DomainAuditEvent(
                "CLINIC_UPDATE", "Clinic", saved.getId().toString(), before, auditSnapshot.of(saved), null));

        return ClinicResponse.from(saved);
    }

    public List<ClinicResponse> list() {
        UUID tenantId = TenantContext.requireTenantId();
        return clinicRepository.findByTenantIdAndDeletedFalse(tenantId).stream()
                .map(ClinicResponse::from)
                .toList();
    }

    public ClinicResponse get(UUID id) {
        return ClinicResponse.from(getOwnedOrThrow(id));
    }

    @Transactional
    public void deactivate(UUID id) {
        Clinic clinic = getOwnedOrThrow(id);
        String before = auditSnapshot.of(clinic);
        clinic.setStatus(Clinic.ClinicStatus.INACTIVE);
        clinicRepository.save(clinic);

        events.publishEvent(new DomainAuditEvent(
                "CLINIC_DEACTIVATE", "Clinic", id.toString(), before, auditSnapshot.of(clinic), null));
    }

    private Clinic getOwnedOrThrow(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return clinicRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("CLINIC_NOT_FOUND", "Clinic not found."));
    }

    private void applyRequest(Clinic clinic, ClinicRequest request) {
        clinic.setName(request.name());
        clinic.setAddressLine(request.addressLine());
        clinic.setCity(request.city());
        clinic.setProvince(request.province());
        clinic.setPostalCode(request.postalCode());
        clinic.setContactNumber(request.contactNumber());
        if (request.timezone() != null && !request.timezone().isBlank()) {
            clinic.setTimezone(request.timezone());
        }
    }

}
