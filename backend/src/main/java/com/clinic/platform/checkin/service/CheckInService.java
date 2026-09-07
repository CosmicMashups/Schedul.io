package com.clinic.platform.checkin.service;

import com.clinic.platform.appointment.domain.Appointment;
import com.clinic.platform.appointment.repository.AppointmentRepository;
import com.clinic.platform.appointment.service.AppointmentService;
import com.clinic.platform.audit.service.DomainAuditEvent;
import com.clinic.platform.checkin.domain.CheckIn;
import com.clinic.platform.checkin.dto.CheckInRequest;
import com.clinic.platform.checkin.dto.CheckInResponse;
import com.clinic.platform.checkin.repository.CheckInRepository;
import com.clinic.platform.common.exception.ApiException;
import com.clinic.platform.common.util.AuditSnapshot;
import com.clinic.platform.queue.domain.Queue;
import com.clinic.platform.queue.domain.QueueTicket;
import com.clinic.platform.queue.service.QueueService;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Section 20's flow, condensed into one transaction: verify appointment → check in →
 * generate queue ticket → advance appointment status twice (CONFIRMED → CHECKED_IN →
 * IN_QUEUE). Splitting this into two user-facing steps (a receptionist explicitly "checking
 * in" vs. a separate "send to queue" action) is a reasonable product variation some clinics
 * may want — flagged here rather than assumed, since collapsing them was the simpler default
 * given no UI/UX spec distinguishing the two steps yet.
 */
@Service
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final QueueService queueService;
    private final ApplicationEventPublisher events;
    private final AuditSnapshot auditSnapshot;

    public CheckInService(CheckInRepository checkInRepository, AppointmentRepository appointmentRepository,
                           AppointmentService appointmentService, QueueService queueService,
                           ApplicationEventPublisher events, AuditSnapshot auditSnapshot) {
        this.checkInRepository = checkInRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentService = appointmentService;
        this.queueService = queueService;
        this.events = events;
        this.auditSnapshot = auditSnapshot;
    }

    @Transactional
    public CheckInResponse checkIn(UUID appointmentId, String checkedInByDisplay, CheckInRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Appointment appointment = appointmentRepository.findByIdAndTenantIdAndDeletedFalse(appointmentId, tenantId)
                .orElseThrow(() -> ApiException.notFound("APPOINTMENT_NOT_FOUND", "Appointment not found."));

        checkInRepository.findByTenantIdAndAppointmentId(tenantId, appointmentId).ifPresent(existing -> {
            throw ApiException.conflict("ALREADY_CHECKED_IN", "This appointment has already been checked in.");
        });

        // advanceStatus() enforces CONFIRMED -> CHECKED_IN via the state machine — an
        // appointment that is still PENDING_CONFIRMATION or already CANCELLED/COMPLETED/etc.
        // is correctly rejected here with a clear error rather than silently proceeding.
        appointmentService.advanceStatus(appointmentId, Appointment.AppointmentStatus.CHECKED_IN, "checked in via " + request.method());

        CheckIn checkIn = new CheckIn();
        checkIn.setAppointmentId(appointmentId);
        checkIn.setPatientId(appointment.getPatientId());
        checkIn.setCheckedInAt(Instant.now());
        checkIn.setMethod(request.method());
        checkIn.setIdentityVerified(request.identityVerified());
        checkIn.setCheckedInBy(checkedInByDisplay);
        CheckIn savedCheckIn = checkInRepository.save(checkIn);

        events.publishEvent(new DomainAuditEvent(
                "PATIENT_CHECK_IN", "Appointment", appointmentId.toString(), null, auditSnapshot.of(savedCheckIn), null));

        Queue doctorQueue = queueService.getOrCreateDoctorQueue(appointment.getClinicId());
        QueueTicket ticket = queueService.enqueue(doctorQueue.getId(), appointmentId, appointment.getPatientId(),
                appointment.getPractitionerId(), 3); // priority 3 = SCHEDULED tier, section 23

        appointmentService.advanceStatus(appointmentId, Appointment.AppointmentStatus.IN_QUEUE,
                "queued as " + ticket.getTicketNumber());

        return CheckInResponse.from(savedCheckIn, ticket.getId(), ticket.getTicketNumber());
    }
}
