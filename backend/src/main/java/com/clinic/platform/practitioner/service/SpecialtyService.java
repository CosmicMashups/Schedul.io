package com.clinic.platform.practitioner.service;

import com.clinic.platform.audit.service.DomainAuditEvent;
import com.clinic.platform.common.exception.ApiException;
import com.clinic.platform.common.util.AuditSnapshot;
import com.clinic.platform.practitioner.domain.Specialty;
import com.clinic.platform.practitioner.dto.SpecialtyRequest;
import com.clinic.platform.practitioner.dto.SpecialtyResponse;
import com.clinic.platform.practitioner.repository.SpecialtyRepository;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Closes a gap flagged during the frontend build: there was no listing endpoint for
 * Specialty, so the staff portal's doctor-creation form had to ask for a raw specialty UUID
 * instead of offering a picker. This is that missing endpoint.
 */
@Service
public class SpecialtyService {

    private final SpecialtyRepository repository;
    private final ApplicationEventPublisher events;
    private final AuditSnapshot auditSnapshot;

    public SpecialtyService(SpecialtyRepository repository, ApplicationEventPublisher events, AuditSnapshot auditSnapshot) {
        this.repository = repository;
        this.events = events;
        this.auditSnapshot = auditSnapshot;
    }

    public List<SpecialtyResponse> list() {
        UUID tenantId = TenantContext.requireTenantId();
        return repository.findByTenantIdAndDeletedFalse(tenantId).stream()
                .map(SpecialtyResponse::from)
                .toList();
    }

    @Transactional
    public SpecialtyResponse create(SpecialtyRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        repository.findByTenantIdAndDeletedFalse(tenantId).stream()
                .filter(s -> s.getCode().equalsIgnoreCase(request.code()))
                .findAny()
                .ifPresent(s -> { throw ApiException.conflict("SPECIALTY_EXISTS", "A specialty with this code already exists."); });

        Specialty specialty = new Specialty();
        specialty.setCode(request.code());
        specialty.setName(request.name());
        Specialty saved = repository.save(specialty);

        events.publishEvent(new DomainAuditEvent(
                "SPECIALTY_CREATE", "Specialty", saved.getId().toString(), null, auditSnapshot.of(saved), null));

        return SpecialtyResponse.from(saved);
    }
}
