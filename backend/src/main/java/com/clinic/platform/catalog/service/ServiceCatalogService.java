package com.clinic.platform.catalog.service;

import com.clinic.platform.audit.service.DomainAuditEvent;
import com.clinic.platform.catalog.domain.ClinicService;
import com.clinic.platform.catalog.dto.ServiceRequest;
import com.clinic.platform.catalog.dto.ServiceResponse;
import com.clinic.platform.catalog.repository.ClinicServiceRepository;
import com.clinic.platform.clinic.domain.Clinic;
import com.clinic.platform.clinic.repository.ClinicRepository;
import com.clinic.platform.common.exception.ApiException;
import com.clinic.platform.common.util.AuditSnapshot;
import com.clinic.platform.practitioner.domain.Specialty;
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
public class ServiceCatalogService {

    private final ClinicServiceRepository serviceRepository;
    private final ClinicRepository clinicRepository;
    private final SpecialtyRepository specialtyRepository;
    private final ApplicationEventPublisher events;
    private final AuditSnapshot auditSnapshot;

    public ServiceCatalogService(ClinicServiceRepository serviceRepository, ClinicRepository clinicRepository,
                                  SpecialtyRepository specialtyRepository, ApplicationEventPublisher events,
                                  AuditSnapshot auditSnapshot) {
        this.serviceRepository = serviceRepository;
        this.clinicRepository = clinicRepository;
        this.specialtyRepository = specialtyRepository;
        this.events = events;
        this.auditSnapshot = auditSnapshot;
    }

    @Transactional
    public ServiceResponse create(ServiceRequest request) {
        ClinicService service = new ClinicService();
        applyRequest(service, request);
        ClinicService saved = serviceRepository.save(service);

        events.publishEvent(new DomainAuditEvent(
                "SERVICE_CREATE", "Service", saved.getId().toString(), null, auditSnapshot.of(saved), null));

        return ServiceResponse.from(saved);
    }

    @Transactional
    public ServiceResponse update(UUID id, ServiceRequest request) {
        ClinicService service = getOwnedOrThrow(id);
        String before = auditSnapshot.of(service);
        applyRequest(service, request);
        ClinicService saved = serviceRepository.save(service);

        events.publishEvent(new DomainAuditEvent(
                "SERVICE_UPDATE", "Service", saved.getId().toString(), before, auditSnapshot.of(saved), null));

        return ServiceResponse.from(saved);
    }

    /** Public directory listing (patient-facing, section 8). */
    public List<ServiceResponse> listActive(UUID clinicId) {
        UUID tenantId = TenantContext.requireTenantId();
        return serviceRepository.findActive(tenantId, clinicId).stream()
                .map(ServiceResponse::from)
                .toList();
    }

    public ServiceResponse get(UUID id) {
        return ServiceResponse.from(getOwnedOrThrow(id));
    }

    @Transactional
    public void deactivate(UUID id) {
        ClinicService service = getOwnedOrThrow(id);
        String before = auditSnapshot.of(service);
        service.setStatus(ClinicService.ServiceStatus.INACTIVE);
        serviceRepository.save(service);

        events.publishEvent(new DomainAuditEvent(
                "SERVICE_DEACTIVATE", "Service", id.toString(), before, auditSnapshot.of(service), null));
    }

    private ClinicService getOwnedOrThrow(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return serviceRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("SERVICE_NOT_FOUND", "Service not found."));
    }

    private void applyRequest(ClinicService service, ServiceRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        service.setName(request.name());
        service.setDescription(request.description());
        service.setDurationMinutes(request.durationMinutes());
        service.setBufferMinutes(request.bufferMinutes());
        service.setPrice(request.price());
        if (request.consultationMode() != null) {
            service.setConsultationMode(request.consultationMode());
        }

        if (request.allowedSpecialtyId() != null) {
            Specialty specialty = specialtyRepository.findById(request.allowedSpecialtyId())
                    .filter(s -> s.getTenantId().equals(tenantId))
                    .orElseThrow(() -> ApiException.badRequest("INVALID_SPECIALTY", "Specialty does not belong to this tenant."));
            service.setAllowedSpecialty(specialty);
        } else {
            service.setAllowedSpecialty(null);
        }

        Set<Clinic> clinics = new HashSet<>(clinicRepository.findAllById(request.clinicIds()));
        clinics.removeIf(c -> !c.getTenantId().equals(tenantId));
        if (clinics.size() != request.clinicIds().size()) {
            throw ApiException.badRequest("INVALID_CLINIC", "One or more clinics do not belong to this tenant.");
        }
        service.setClinics(clinics);
    }
}
