package com.clinic.platform.scheduling.service;

import com.clinic.platform.catalog.domain.ClinicService;
import com.clinic.platform.catalog.repository.ClinicServiceRepository;
import com.clinic.platform.common.exception.ApiException;
import com.clinic.platform.practitioner.domain.Practitioner;
import com.clinic.platform.practitioner.repository.PractitionerRepository;
import com.clinic.platform.scheduling.dto.AvailabilityResponse;
import com.clinic.platform.scheduling.dto.SlotResponse;
import com.clinic.platform.scheduling.repository.SlotRepository;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Section 40: the availability API. Deliberately does NOT filter returned slots by exact
 * duration match against the service's durationMinutes yet — slots are currently generated
 * at the ScheduleRule's own slotDurationMinutes, which may differ from a given Service's
 * duration. Reconciling that (splitting/merging slots per service, or generating
 * service-specific slot grids) is flagged as a follow-up once real booking volume shows which
 * approach clinics actually need; for now this returns every FREE slot for a practitioner
 * capable of performing the service, and lets the booking step (Milestone 4) validate fit.
 */
@Service
public class AvailabilityService {

    private final SlotRepository slotRepository;
    private final PractitionerRepository practitionerRepository;
    private final ClinicServiceRepository serviceRepository;

    public AvailabilityService(SlotRepository slotRepository, PractitionerRepository practitionerRepository,
                                ClinicServiceRepository serviceRepository) {
        this.slotRepository = slotRepository;
        this.practitionerRepository = practitionerRepository;
        this.serviceRepository = serviceRepository;
    }

    public AvailabilityResponse getAvailability(UUID practitionerId, UUID serviceId, UUID clinicId,
                                                 LocalDate from, LocalDate to) {
        UUID tenantId = TenantContext.requireTenantId();

        Practitioner practitioner = practitionerRepository.findByIdAndTenantIdAndDeletedFalse(practitionerId, tenantId)
                .orElseThrow(() -> ApiException.notFound("PRACTITIONER_NOT_FOUND", "Practitioner not found."));

        if (serviceId != null) {
            ClinicService service = serviceRepository.findByIdAndTenantIdAndDeletedFalse(serviceId, tenantId)
                    .orElseThrow(() -> ApiException.notFound("SERVICE_NOT_FOUND", "Service not found."));
            if (service.getAllowedSpecialty() != null
                    && practitioner.getSpecialties().stream().noneMatch(s -> s.getId().equals(service.getAllowedSpecialty().getId()))) {
                throw ApiException.badRequest("PRACTITIONER_SERVICE_MISMATCH",
                        "This practitioner does not offer the requested service.");
            }
        }

        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        var slots = slotRepository.findFreeSlots(tenantId, practitionerId, clinicId, fromInstant, toInstant).stream()
                .map(SlotResponse::from)
                .toList();

        return new AvailabilityResponse(practitionerId, serviceId, slots);
    }
}
