package com.clinic.platform.appointment.service;

import com.clinic.platform.appointment.domain.AppointmentType;
import com.clinic.platform.appointment.dto.AppointmentTypeRequest;
import com.clinic.platform.appointment.dto.AppointmentTypeResponse;
import com.clinic.platform.appointment.repository.AppointmentTypeRepository;
import com.clinic.platform.audit.service.DomainAuditEvent;
import com.clinic.platform.common.exception.ApiException;
import com.clinic.platform.common.util.AuditSnapshot;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AppointmentTypeService {

    private final AppointmentTypeRepository repository;
    private final ApplicationEventPublisher events;
    private final AuditSnapshot auditSnapshot;

    public AppointmentTypeService(AppointmentTypeRepository repository, ApplicationEventPublisher events, AuditSnapshot auditSnapshot) {
        this.repository = repository;
        this.events = events;
        this.auditSnapshot = auditSnapshot;
    }

    @Transactional
    public AppointmentTypeResponse create(AppointmentTypeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        repository.findByTenantIdAndCodeAndDeletedFalse(tenantId, request.code()).ifPresent(t -> {
            throw ApiException.conflict("APPOINTMENT_TYPE_EXISTS", "An appointment type with this code already exists.");
        });

        AppointmentType type = new AppointmentType();
        applyRequest(type, request);
        AppointmentType saved = repository.save(type);

        events.publishEvent(new DomainAuditEvent(
                "APPOINTMENT_TYPE_CREATE", "AppointmentType", saved.getId().toString(), null, auditSnapshot.of(saved), null));

        return AppointmentTypeResponse.from(saved);
    }

    public List<AppointmentTypeResponse> listActive() {
        UUID tenantId = TenantContext.requireTenantId();
        return repository.findByTenantIdAndActiveTrueAndDeletedFalse(tenantId).stream()
                .map(AppointmentTypeResponse::from)
                .toList();
    }

    @Transactional
    public void deactivate(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        AppointmentType type = repository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("APPOINTMENT_TYPE_NOT_FOUND", "Appointment type not found."));

        String before = auditSnapshot.of(type);
        type.setActive(false);
        repository.save(type);

        events.publishEvent(new DomainAuditEvent(
                "APPOINTMENT_TYPE_DEACTIVATE", "AppointmentType", id.toString(), before, auditSnapshot.of(type), null));
    }

    private void applyRequest(AppointmentType type, AppointmentTypeRequest request) {
        type.setCode(request.code());
        type.setName(request.name());
        type.setConfirmationPolicy(request.confirmationPolicy());
        type.setCancellationWindowHours(request.cancellationWindowHours());
        type.setAdvanceBookingLimitDays(request.advanceBookingLimitDays());
    }
}
