package com.clinic.platform.appointment.service;

import com.clinic.platform.appointment.domain.Appointment;
import com.clinic.platform.appointment.domain.Appointment.AppointmentStatus;
import com.clinic.platform.appointment.domain.AppointmentStatusHistory;
import com.clinic.platform.appointment.domain.AppointmentType;
import com.clinic.platform.appointment.dto.*;
import com.clinic.platform.appointment.repository.AppointmentRepository;
import com.clinic.platform.appointment.repository.AppointmentStatusHistoryRepository;
import com.clinic.platform.appointment.repository.AppointmentTypeRepository;
import com.clinic.platform.audit.service.DomainAuditEvent;
import com.clinic.platform.catalog.domain.ClinicService;
import com.clinic.platform.catalog.repository.ClinicServiceRepository;
import com.clinic.platform.clinic.repository.ClinicRepository;
import com.clinic.platform.common.exception.ApiException;
import com.clinic.platform.common.util.AuditSnapshot;
import com.clinic.platform.patient.repository.PatientRepository;
import com.clinic.platform.practitioner.domain.Practitioner;
import com.clinic.platform.practitioner.repository.PractitionerRepository;
import com.clinic.platform.scheduling.domain.Slot;
import com.clinic.platform.scheduling.repository.SlotRepository;
import com.clinic.platform.scheduling.service.SlotHoldService;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Section 41's booking transaction + section 14's state machine + sections 16-18's
 * confirmation/cancellation/reschedule flows, all in one service since they share the same
 * invariant: every mutation to an Appointment's status must go through
 * {@link AppointmentStateMachine#assertTransition} and be recorded in both
 * {@link AppointmentStatusHistory} (fast timeline reads, section 33) and the generic
 * {@link DomainAuditEvent} (compliance trail, section 45).
 *
 * Section 42 (event-driven): this service does NOT call notification/SMS code directly. It
 * only publishes DomainAuditEvent; a NotificationService subscribing to appointment-lifecycle
 * events is Milestone 8's job — wiring it in now would be premature given no notification
 * module exists yet.
 */
@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentTypeRepository appointmentTypeRepository;
    private final AppointmentStatusHistoryRepository historyRepository;
    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final ClinicRepository clinicRepository;
    private final ClinicServiceRepository serviceRepository;
    private final SlotRepository slotRepository;
    private final SlotHoldService slotHoldService;
    private final ApplicationEventPublisher events;
    private final AuditSnapshot auditSnapshot;

    public AppointmentService(AppointmentRepository appointmentRepository, AppointmentTypeRepository appointmentTypeRepository,
                               AppointmentStatusHistoryRepository historyRepository, PatientRepository patientRepository,
                               PractitionerRepository practitionerRepository, ClinicRepository clinicRepository,
                               ClinicServiceRepository serviceRepository, SlotRepository slotRepository,
                               SlotHoldService slotHoldService, ApplicationEventPublisher events, AuditSnapshot auditSnapshot) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentTypeRepository = appointmentTypeRepository;
        this.historyRepository = historyRepository;
        this.patientRepository = patientRepository;
        this.practitionerRepository = practitionerRepository;
        this.clinicRepository = clinicRepository;
        this.serviceRepository = serviceRepository;
        this.slotRepository = slotRepository;
        this.slotHoldService = slotHoldService;
        this.events = events;
        this.auditSnapshot = auditSnapshot;
    }

    /**
     * Section 41's booking pipeline (validate → lock slot → create appointment → commit →
     * publish event), steps 1-17. Reminder scheduling (step 18) is Milestone 8's job.
     *
     * The appointment row is inserted BEFORE the slot is booked so its generated UUID can be
     * stamped onto the Slot as appointment_id; if the subsequent slot-booking call throws
     * (slot no longer available), the whole @Transactional method rolls back and the
     * appointment insert is undone too — see SlotHoldService.book()'s javadoc for why this
     * ordering is safe.
     */
    @Transactional
    public AppointmentResponse book(UUID requestingUserId, BookAppointmentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        patientRepository.findByIdAndTenantIdAndDeletedFalse(request.patientId(), tenantId)
                .orElseThrow(() -> ApiException.badRequest("INVALID_PATIENT", "Patient not found."));
        Practitioner practitioner = practitionerRepository.findByIdAndTenantIdAndDeletedFalse(request.practitionerId(), tenantId)
                .orElseThrow(() -> ApiException.badRequest("INVALID_PRACTITIONER", "Practitioner not found."));
        clinicRepository.findByIdAndTenantIdAndDeletedFalse(request.clinicId(), tenantId)
                .orElseThrow(() -> ApiException.badRequest("INVALID_CLINIC", "Clinic not found."));
        ClinicService service = serviceRepository.findByIdAndTenantIdAndDeletedFalse(request.serviceId(), tenantId)
                .orElseThrow(() -> ApiException.badRequest("INVALID_SERVICE", "Service not found."));
        AppointmentType type = appointmentTypeRepository.findByIdAndTenantIdAndDeletedFalse(request.appointmentTypeId(), tenantId)
                .filter(AppointmentType::isActive)
                .orElseThrow(() -> ApiException.badRequest("INVALID_APPOINTMENT_TYPE", "Appointment type not found or inactive."));

        if (service.getAllowedSpecialty() != null
                && practitioner.getSpecialties().stream().noneMatch(s -> s.getId().equals(service.getAllowedSpecialty().getId()))) {
            throw ApiException.badRequest("PRACTITIONER_SERVICE_MISMATCH", "This practitioner does not offer the requested service.");
        }

        // Read-only fetch to populate scheduledStart/End on the Appointment row; the actual
        // safety-critical lock happens inside slotHoldService.book() below.
        Slot slotPreview = slotRepository.lockForUpdate(request.slotId(), tenantId)
                .orElseThrow(() -> ApiException.badRequest("INVALID_SLOT", "Slot not found."));
        if (!slotPreview.getPractitionerId().equals(request.practitionerId())) {
            throw ApiException.badRequest("SLOT_PRACTITIONER_MISMATCH", "Slot does not belong to the requested practitioner.");
        }

        if (type.getAdvanceBookingLimitDays() != null) {
            long daysOut = ChronoUnit.DAYS.between(Instant.now(), slotPreview.getStartAt());
            if (daysOut > type.getAdvanceBookingLimitDays()) {
                throw ApiException.badRequest("ADVANCE_BOOKING_LIMIT_EXCEEDED",
                        "This appointment type cannot be booked more than " + type.getAdvanceBookingLimitDays() + " days in advance.");
            }
        }

        Appointment appointment = new Appointment();
        appointment.setPatientId(request.patientId());
        appointment.setPractitionerId(request.practitionerId());
        appointment.setClinicId(request.clinicId());
        appointment.setServiceId(request.serviceId());
        appointment.setAppointmentTypeId(request.appointmentTypeId());
        appointment.setSlotId(request.slotId());
        appointment.setScheduledStart(slotPreview.getStartAt());
        appointment.setScheduledEnd(slotPreview.getEndAt());
        appointment.setReason(request.reason());
        appointment.setSource(request.source() != null ? request.source() : Appointment.AppointmentSource.ONLINE);

        boolean instantConfirm = type.getConfirmationPolicy() == AppointmentType.ConfirmationPolicy.INSTANT_CONFIRMATION;
        appointment.setStatus(instantConfirm ? AppointmentStatus.CONFIRMED : AppointmentStatus.PENDING_CONFIRMATION);
        if (instantConfirm) {
            appointment.setConfirmedAt(Instant.now());
        }

        Appointment saved = appointmentRepository.save(appointment);

        // Now that the appointment has an id, actually claim the slot under lock.
        slotHoldService.book(request.slotId(), requestingUserId, saved.getId(), false);

        recordHistory(saved.getId(), null, saved.getStatus(), "booked");
        events.publishEvent(new DomainAuditEvent(
                "APPOINTMENT_BOOK", "Appointment", saved.getId().toString(), null, auditSnapshot.of(saved),
                "policy=" + type.getConfirmationPolicy()));

        return AppointmentResponse.from(saved);
    }

    /** Section 16: staff/doctor approval step for PENDING_CONFIRMATION appointments. */
    @Transactional
    public AppointmentResponse confirm(UUID appointmentId) {
        Appointment appointment = getOwnedOrThrow(appointmentId);
        AppointmentStatus from = appointment.getStatus();
        AppointmentStateMachine.assertTransition(from, AppointmentStatus.CONFIRMED);

        String before = auditSnapshot.of(appointment);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setConfirmedAt(Instant.now());
        Appointment saved = appointmentRepository.save(appointment);

        recordHistory(saved.getId(), from, saved.getStatus(), "confirmed");
        events.publishEvent(new DomainAuditEvent(
                "APPOINTMENT_CONFIRM", "Appointment", saved.getId().toString(), before, auditSnapshot.of(saved), null));

        return AppointmentResponse.from(saved);
    }

    /** Section 16: staff/doctor declines a pending request — releases the slot back to FREE. */
    @Transactional
    public AppointmentResponse reject(UUID appointmentId, RejectAppointmentRequest request) {
        Appointment appointment = getOwnedOrThrow(appointmentId);
        AppointmentStatus from = appointment.getStatus();
        AppointmentStateMachine.assertTransition(from, AppointmentStatus.REJECTED);

        String before = auditSnapshot.of(appointment);
        appointment.setStatus(AppointmentStatus.REJECTED);
        Appointment saved = appointmentRepository.save(appointment);
        slotHoldService.releaseBooked(appointment.getSlotId(), "appointment rejected");

        recordHistory(saved.getId(), from, saved.getStatus(), request.reason());
        events.publishEvent(new DomainAuditEvent(
                "APPOINTMENT_REJECT", "Appointment", saved.getId().toString(), before, auditSnapshot.of(saved), request.reason()));

        return AppointmentResponse.from(saved);
    }

    /** Section 18: cancellation — releases the slot regardless of who cancels (patient or staff). */
    @Transactional
    public AppointmentResponse cancel(UUID appointmentId, String cancelledByDisplay, CancelAppointmentRequest request) {
        Appointment appointment = getOwnedOrThrow(appointmentId);
        AppointmentStatus from = appointment.getStatus();
        AppointmentStateMachine.assertTransition(from, AppointmentStatus.CANCELLED);

        String before = auditSnapshot.of(appointment);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledBy(cancelledByDisplay);
        appointment.setCancellationReason(request.reason());
        appointment.setCancellationNote(request.note());
        appointment.setCancelledAt(Instant.now());
        Appointment saved = appointmentRepository.save(appointment);
        slotHoldService.releaseBooked(appointment.getSlotId(), "appointment cancelled: " + request.reason());

        recordHistory(saved.getId(), from, saved.getStatus(), request.reason().name());
        events.publishEvent(new DomainAuditEvent(
                "APPOINTMENT_CANCEL", "Appointment", saved.getId().toString(), before, auditSnapshot.of(saved),
                request.reason() + (request.note() != null ? ": " + request.note() : "")));

        return AppointmentResponse.from(saved);
    }

    /**
     * Section 17: reschedule creates a NEW Appointment rather than mutating the old one's
     * time — the old row becomes RESCHEDULED and links forward via replacementAppointmentId;
     * the new row links backward via previousAppointmentId. Inherits the original's service/
     * appointmentType/source; the confirmation policy of the target appointment type is
     * intentionally not re-evaluated here — a reschedule of an already-confirmed appointment
     * stays confirmed rather than re-entering an approval queue.
     */
    @Transactional
    public AppointmentResponse reschedule(UUID requestingUserId, UUID appointmentId, RescheduleAppointmentRequest request) {
        Appointment original = getOwnedOrThrow(appointmentId);
        AppointmentStatus from = original.getStatus();
        AppointmentStateMachine.assertTransition(from, AppointmentStatus.RESCHEDULED);

        UUID tenantId = TenantContext.requireTenantId();
        Slot newSlotPreview = slotRepository.lockForUpdate(request.newSlotId(), tenantId)
                .orElseThrow(() -> ApiException.badRequest("INVALID_SLOT", "Slot not found."));
        if (!newSlotPreview.getPractitionerId().equals(original.getPractitionerId())) {
            throw ApiException.badRequest("SLOT_PRACTITIONER_MISMATCH", "New slot must belong to the same practitioner.");
        }

        Appointment replacement = new Appointment();
        replacement.setPatientId(original.getPatientId());
        replacement.setPractitionerId(original.getPractitionerId());
        replacement.setClinicId(original.getClinicId());
        replacement.setServiceId(original.getServiceId());
        replacement.setAppointmentTypeId(original.getAppointmentTypeId());
        replacement.setSlotId(request.newSlotId());
        replacement.setScheduledStart(newSlotPreview.getStartAt());
        replacement.setScheduledEnd(newSlotPreview.getEndAt());
        replacement.setReason(original.getReason());
        replacement.setSource(original.getSource());
        replacement.setPreviousAppointmentId(original.getId());
        replacement.setStatus(AppointmentStatus.CONFIRMED);
        replacement.setConfirmedAt(Instant.now());

        Appointment savedReplacement = appointmentRepository.save(replacement);
        slotHoldService.book(request.newSlotId(), requestingUserId, savedReplacement.getId(), false);

        String before = auditSnapshot.of(original);
        original.setStatus(AppointmentStatus.RESCHEDULED);
        original.setReplacementAppointmentId(savedReplacement.getId());
        Appointment savedOriginal = appointmentRepository.save(original);
        slotHoldService.releaseBooked(original.getSlotId(), "rescheduled to " + savedReplacement.getId());

        recordHistory(savedOriginal.getId(), from, savedOriginal.getStatus(), request.reason());
        recordHistory(savedReplacement.getId(), null, savedReplacement.getStatus(), "created via reschedule of " + original.getId());
        events.publishEvent(new DomainAuditEvent(
                "APPOINTMENT_RESCHEDULE", "Appointment", savedOriginal.getId().toString(), before, auditSnapshot.of(savedOriginal),
                "replacementAppointmentId=" + savedReplacement.getId()));

        return AppointmentResponse.from(savedReplacement);
    }

    /** Section 19: staff marks a confirmed-but-absent patient — slot stays BOOKED for historical accuracy. */
    @Transactional
    public AppointmentResponse markNoShow(UUID appointmentId) {
        Appointment appointment = getOwnedOrThrow(appointmentId);
        AppointmentStatus from = appointment.getStatus();
        AppointmentStateMachine.assertTransition(from, AppointmentStatus.NO_SHOW);

        String before = auditSnapshot.of(appointment);
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        Appointment saved = appointmentRepository.save(appointment);

        recordHistory(saved.getId(), from, saved.getStatus(), "marked no-show");
        events.publishEvent(new DomainAuditEvent(
                "APPOINTMENT_NO_SHOW", "Appointment", saved.getId().toString(), before, auditSnapshot.of(saved), null));

        return AppointmentResponse.from(saved);
    }

    /**
     * Narrow entry point for the patient-flow modules (check-in, queue — Milestone 7) to
     * advance an Appointment's status without duplicating {@link AppointmentStateMachine}'s
     * transition table outside this service. Intentionally does not accept arbitrary
     * transitions — CheckInService/QueueService each call this with the specific status their
     * own domain event implies (e.g. "patient checked in" → CHECKED_IN), not a raw enum from
     * the HTTP layer.
     */
    @Transactional
    public void advanceStatus(UUID appointmentId, AppointmentStatus to, String reason) {
        Appointment appointment = getOwnedOrThrow(appointmentId);
        AppointmentStatus from = appointment.getStatus();
        AppointmentStateMachine.assertTransition(from, to);

        String before = auditSnapshot.of(appointment);
        appointment.setStatus(to);
        Appointment saved = appointmentRepository.save(appointment);

        recordHistory(saved.getId(), from, to, reason);
        events.publishEvent(new DomainAuditEvent(
                "APPOINTMENT_STATUS_ADVANCE", "Appointment", saved.getId().toString(), before, auditSnapshot.of(saved), reason));
    }

    /** Read-only variant for callers (e.g. QueueService) that need to know the current status without transitioning it. */
    public AppointmentStatus getStatus(UUID appointmentId) {
        return getOwnedOrThrow(appointmentId).getStatus();
    }

    public AppointmentResponse get(UUID id) {
        return AppointmentResponse.from(getOwnedOrThrow(id));
    }

    public List<AppointmentResponse> listForPatient(UUID patientId) {
        UUID tenantId = TenantContext.requireTenantId();
        return appointmentRepository.findByTenantIdAndPatientIdAndDeletedFalseOrderByScheduledStartDesc(tenantId, patientId)
                .stream().map(AppointmentResponse::from).toList();
    }

    /** Section 32 staff search. */
    public List<AppointmentResponse> search(UUID practitionerId, UUID clinicId, Appointment.AppointmentStatus status,
                                             Instant fromDate, Instant toDate) {
        UUID tenantId = TenantContext.requireTenantId();
        return appointmentRepository.search(tenantId, practitionerId, clinicId, status, fromDate, toDate)
                .stream().map(AppointmentResponse::from).toList();
    }

    private Appointment getOwnedOrThrow(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return appointmentRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("APPOINTMENT_NOT_FOUND", "Appointment not found."));
    }

    private void recordHistory(UUID appointmentId, AppointmentStatus from, AppointmentStatus to, String reason) {
        AppointmentStatusHistory history = new AppointmentStatusHistory();
        history.setAppointmentId(appointmentId);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setReason(reason);
        historyRepository.save(history);
    }
}
