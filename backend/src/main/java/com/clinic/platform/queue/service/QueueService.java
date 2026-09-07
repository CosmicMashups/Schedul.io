package com.clinic.platform.queue.service;

import com.clinic.platform.appointment.domain.Appointment;
import com.clinic.platform.appointment.service.AppointmentService;
import com.clinic.platform.audit.service.DomainAuditEvent;
import com.clinic.platform.common.exception.ApiException;
import com.clinic.platform.common.util.AuditSnapshot;
import com.clinic.platform.queue.domain.Queue;
import com.clinic.platform.queue.domain.QueueTicket;
import com.clinic.platform.queue.dto.QueueResponse;
import com.clinic.platform.queue.dto.QueueTicketResponse;
import com.clinic.platform.queue.repository.QueueRepository;
import com.clinic.platform.queue.repository.QueueTicketRepository;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Section 21 (multiple named queues, not one global queue), section 22 (ticket lifecycle),
 * section 23 (priority is configurable/transparent - an int column, not a hardcoded enum
 * ranking - and every reorder-adjacent action here publishes a DomainAuditEvent per that
 * section's explicit warning against silent manipulation).
 *
 * All ticket mutations go through {@link QueueTicketRepository#lockForUpdate}, mirroring the
 * scheduling module's SELECT ... FOR UPDATE pattern (section 13) - two receptionists calling
 * "call next" concurrently must not both claim the same ticket.
 *
 * Where a ticket has an appointmentId (i.e. it did not originate from a walk-in), starting
 * service and completing service also advance the linked Appointment
 * (IN_QUEUE→IN_CONSULTATION, IN_CONSULTATION→COMPLETED) via AppointmentService.advanceStatus
 * — the single narrow entry point Milestone 4 exposed for exactly this purpose, rather than
 * this module reaching into Appointment's fields directly.
 */
@Service
public class QueueService {

    private final QueueRepository queueRepository;
    private final QueueTicketRepository ticketRepository;
    private final AppointmentService appointmentService;
    private final ApplicationEventPublisher events;
    private final AuditSnapshot auditSnapshot;

    public QueueService(QueueRepository queueRepository, QueueTicketRepository ticketRepository,
                         AppointmentService appointmentService, ApplicationEventPublisher events, AuditSnapshot auditSnapshot) {
        this.queueRepository = queueRepository;
        this.ticketRepository = ticketRepository;
        this.appointmentService = appointmentService;
        this.events = events;
        this.auditSnapshot = auditSnapshot;
    }

    /** Auto-provisions the clinic's DOCTOR queue on first use so staff don't need a manual setup step for the common case. */
    @Transactional
    public Queue getOrCreateDoctorQueue(UUID clinicId) {
        UUID tenantId = TenantContext.requireTenantId();
        return queueRepository.findByTenantIdAndClinicIdAndTypeAndDeletedFalse(tenantId, clinicId, Queue.QueueType.DOCTOR)
                .orElseGet(() -> {
                    Queue queue = new Queue();
                    queue.setClinicId(clinicId);
                    queue.setName("Doctor Queue");
                    queue.setType(Queue.QueueType.DOCTOR);
                    return queueRepository.save(queue);
                });
    }

    public List<QueueResponse> listForClinic(UUID clinicId) {
        UUID tenantId = TenantContext.requireTenantId();
        return queueRepository.findByTenantIdAndClinicIdAndDeletedFalse(tenantId, clinicId).stream()
                .map(QueueResponse::from)
                .toList();
    }

    /** Section 24/25's dashboards: active (WAITING/CALLED) tickets, priority + FIFO ordered. */
    public List<QueueTicketResponse> listActiveTickets(UUID queueId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ticketRepository.findActiveByQueueOrderedByPriority(tenantId, queueId).stream()
                .map(QueueTicketResponse::from)
                .toList();
    }

    /**
     * Creates a WAITING ticket. Called by CheckInService right after check-in (appointment
     * flow) or directly by staff for a walk-in (section 28 - appointmentId is null in that
     * case). priority defaults to 3 (SCHEDULED tier, section 23); staff can override for
     * emergencies via a future admin action - not exposed on this endpoint yet since no UI
     * consumes it.
     */
    @Transactional
    public QueueTicket enqueue(UUID queueId, UUID appointmentId, UUID patientId, UUID practitionerId, int priority) {
        UUID tenantId = TenantContext.requireTenantId();
        Queue queue = queueRepository.findByIdAndTenantIdAndDeletedFalse(queueId, tenantId)
                .orElseThrow(() -> ApiException.notFound("QUEUE_NOT_FOUND", "Queue not found."));
        if (queue.getStatus() != Queue.QueueStatus.OPEN) {
            throw ApiException.conflict("QUEUE_CLOSED", "This queue is closed.");
        }

        QueueTicket ticket = new QueueTicket();
        ticket.setQueueId(queueId);
        ticket.setAppointmentId(appointmentId);
        ticket.setPatientId(patientId);
        ticket.setPractitionerId(practitionerId);
        ticket.setPriority(priority);
        ticket.setTicketNumber(generateTicketNumber(tenantId, queue));
        ticket.setStatus(QueueTicket.TicketStatus.WAITING);

        QueueTicket saved = ticketRepository.save(ticket);
        events.publishEvent(new DomainAuditEvent(
                "QUEUE_TICKET_CREATE", "QueueTicket", saved.getId().toString(), null, auditSnapshot.of(saved), null));
        return saved;
    }

    /** Section 24: doctor/reception "call next" action. */
    @Transactional
    public QueueTicketResponse callNext(UUID queueId) {
        UUID tenantId = TenantContext.requireTenantId();
        List<QueueTicket> candidates = ticketRepository.findActiveByQueueOrderedByPriority(tenantId, queueId).stream()
                .filter(t -> t.getStatus() == QueueTicket.TicketStatus.WAITING)
                .toList();
        if (candidates.isEmpty()) {
            throw ApiException.notFound("QUEUE_EMPTY", "No waiting tickets in this queue.");
        }

        QueueTicket ticket = ticketRepository.lockForUpdate(candidates.get(0).getId(), tenantId)
                .orElseThrow(() -> ApiException.notFound("QUEUE_TICKET_NOT_FOUND", "Queue ticket not found."));
        if (ticket.getStatus() != QueueTicket.TicketStatus.WAITING) {
            throw ApiException.conflict("TICKET_NOT_WAITING", "This ticket is no longer waiting (status: " + ticket.getStatus() + ").");
        }

        String before = auditSnapshot.of(ticket);
        ticket.setStatus(QueueTicket.TicketStatus.CALLED);
        ticket.setCalledAt(Instant.now());
        QueueTicket saved = ticketRepository.save(ticket);

        events.publishEvent(new DomainAuditEvent(
                "QUEUE_TICKET_CALL", "QueueTicket", saved.getId().toString(), before, auditSnapshot.of(saved), null));
        return QueueTicketResponse.from(saved);
    }

    /** Section 24: doctor starts the consultation for a called ticket. */
    @Transactional
    public QueueTicket startServing(UUID ticketId) {
        QueueTicket ticket = lockOwnedOrThrow(ticketId);
        if (ticket.getStatus() != QueueTicket.TicketStatus.CALLED && ticket.getStatus() != QueueTicket.TicketStatus.WAITING) {
            throw ApiException.conflict("TICKET_NOT_CALLABLE", "Ticket must be WAITING or CALLED to start serving (status: " + ticket.getStatus() + ").");
        }

        String before = auditSnapshot.of(ticket);
        ticket.setStatus(QueueTicket.TicketStatus.SERVING);
        ticket.setServingStartedAt(Instant.now());
        QueueTicket saved = ticketRepository.save(ticket);

        if (saved.getAppointmentId() != null) {
            appointmentService.advanceStatus(saved.getAppointmentId(), Appointment.AppointmentStatus.IN_CONSULTATION,
                    "queue ticket " + saved.getTicketNumber() + " started serving");
        }

        events.publishEvent(new DomainAuditEvent(
                "QUEUE_TICKET_SERVE", "QueueTicket", saved.getId().toString(), before, auditSnapshot.of(saved), null));
        return saved;
    }

    /** Section 24: doctor completes the visit. */
    @Transactional
    public QueueTicket complete(UUID ticketId) {
        QueueTicket ticket = lockOwnedOrThrow(ticketId);
        if (ticket.getStatus() != QueueTicket.TicketStatus.SERVING) {
            throw ApiException.conflict("TICKET_NOT_SERVING", "Ticket must be SERVING to complete (status: " + ticket.getStatus() + ").");
        }

        String before = auditSnapshot.of(ticket);
        ticket.setStatus(QueueTicket.TicketStatus.COMPLETED);
        ticket.setCompletedAt(Instant.now());
        QueueTicket saved = ticketRepository.save(ticket);

        if (saved.getAppointmentId() != null) {
            appointmentService.advanceStatus(saved.getAppointmentId(), Appointment.AppointmentStatus.COMPLETED,
                    "queue ticket " + saved.getTicketNumber() + " completed");
        }

        events.publishEvent(new DomainAuditEvent(
                "QUEUE_TICKET_COMPLETE", "QueueTicket", saved.getId().toString(), before, auditSnapshot.of(saved), null));
        return saved;
    }

    /** Section 24: doctor skips a called-but-absent patient - they can be re-queued by staff at the back of the line if they return. */
    @Transactional
    public QueueTicket skip(UUID ticketId) {
        QueueTicket ticket = lockOwnedOrThrow(ticketId);
        if (ticket.getStatus() != QueueTicket.TicketStatus.CALLED && ticket.getStatus() != QueueTicket.TicketStatus.WAITING) {
            throw ApiException.conflict("TICKET_NOT_SKIPPABLE", "Ticket must be WAITING or CALLED to skip (status: " + ticket.getStatus() + ").");
        }

        String before = auditSnapshot.of(ticket);
        ticket.setStatus(QueueTicket.TicketStatus.SKIPPED);
        QueueTicket saved = ticketRepository.save(ticket);

        events.publishEvent(new DomainAuditEvent(
                "QUEUE_TICKET_SKIP", "QueueTicket", saved.getId().toString(), before, auditSnapshot.of(saved), null));
        return saved;
    }

    /** Staff-initiated cancellation of a ticket (e.g. patient left, duplicate ticket). Does not change the linked Appointment's status — use {@link #leaveWithoutBeingSeen} when the patient physically left before being seen. */
    @Transactional
    public QueueTicket cancel(UUID ticketId, String reason) {
        QueueTicket ticket = lockOwnedOrThrow(ticketId);
        if (ticket.getStatus() == QueueTicket.TicketStatus.COMPLETED || ticket.getStatus() == QueueTicket.TicketStatus.CANCELLED) {
            throw ApiException.conflict("TICKET_ALREADY_TERMINAL", "This ticket is already " + ticket.getStatus() + ".");
        }

        String before = auditSnapshot.of(ticket);
        ticket.setStatus(QueueTicket.TicketStatus.CANCELLED);
        QueueTicket saved = ticketRepository.save(ticket);

        events.publishEvent(new DomainAuditEvent(
                "QUEUE_TICKET_CANCEL", "QueueTicket", saved.getId().toString(), before, auditSnapshot.of(saved), reason));
        return saved;
    }

    /**
     * Section 14's CHECKED_IN/IN_QUEUE → LEFT_WITHOUT_BEING_SEEN transition: the patient
     * checked in but left before being called or seen. Cancels the ticket AND, if linked to
     * an Appointment, advances it to LEFT_WITHOUT_BEING_SEEN — distinct from a plain
     * {@link #cancel}, which leaves the Appointment's status untouched (e.g. duplicate ticket
     * cleanup shouldn't imply the patient abandoned their visit).
     */
    @Transactional
    public QueueTicket leaveWithoutBeingSeen(UUID ticketId) {
        QueueTicket ticket = lockOwnedOrThrow(ticketId);
        if (ticket.getStatus() == QueueTicket.TicketStatus.COMPLETED || ticket.getStatus() == QueueTicket.TicketStatus.CANCELLED) {
            throw ApiException.conflict("TICKET_ALREADY_TERMINAL", "This ticket is already " + ticket.getStatus() + ".");
        }

        String before = auditSnapshot.of(ticket);
        ticket.setStatus(QueueTicket.TicketStatus.CANCELLED);
        QueueTicket saved = ticketRepository.save(ticket);

        if (saved.getAppointmentId() != null) {
            appointmentService.advanceStatus(saved.getAppointmentId(), Appointment.AppointmentStatus.LEFT_WITHOUT_BEING_SEEN,
                    "queue ticket " + saved.getTicketNumber() + ": left without being seen");
        }

        events.publishEvent(new DomainAuditEvent(
                "QUEUE_TICKET_LEFT_WITHOUT_BEING_SEEN", "QueueTicket", saved.getId().toString(), before, auditSnapshot.of(saved), null));
        return saved;
    }

    private QueueTicket lockOwnedOrThrow(UUID ticketId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ticketRepository.lockForUpdate(ticketId, tenantId)
                .orElseThrow(() -> ApiException.notFound("QUEUE_TICKET_NOT_FOUND", "Queue ticket not found."));
    }

    private String generateTicketNumber(UUID tenantId, Queue queue) {
        Instant dayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = dayStart.plusSeconds(86400);
        long countToday = ticketRepository.countByTenantIdAndQueueIdAndCreatedAtBetween(tenantId, queue.getId(), dayStart, dayEnd);
        char prefix = Character.toUpperCase(queue.getType().name().charAt(0));
        return prefix + "-" + String.format("%03d", countToday + 1);
    }
}
