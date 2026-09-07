package com.clinic.platform.notification.service;

import com.clinic.platform.appointment.domain.Appointment;
import com.clinic.platform.appointment.repository.AppointmentRepository;
import com.clinic.platform.audit.service.DomainAuditEvent;
import com.clinic.platform.catalog.repository.ClinicServiceRepository;
import com.clinic.platform.clinic.repository.ClinicRepository;
import com.clinic.platform.notification.domain.NotificationEvent;
import com.clinic.platform.patient.domain.Patient;
import com.clinic.platform.patient.repository.PatientRepository;
import com.clinic.platform.practitioner.repository.PractitionerRepository;
import com.clinic.platform.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Section 42: subscribes to the same DomainAuditEvent bus AuditEventListener uses for
 * compliance logging - one appointment action produces both an audit record and, where a
 * matching event maps, a notification, without AppointmentService knowing notifications
 * exist. AFTER_COMMIT, same reasoning as AuditEventListener: never notify about a change
 * that got rolled back.
 *
 * Deliberately maps only a fixed set of AppointmentService audit actions (see the switch
 * below) - any action not listed is silently ignored here, which is correct: e.g.
 * APPOINTMENT_STATUS_ADVANCE (check-in/queue progression) has no patient-facing notification
 * defined in section 30's list, so it's not wired.
 */
@Component
public class AppointmentNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentNotificationListener.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final ClinicRepository clinicRepository;
    private final ClinicServiceRepository serviceRepository;
    private final NotificationDispatchService dispatchService;

    public AppointmentNotificationListener(AppointmentRepository appointmentRepository, PatientRepository patientRepository,
                                            PractitionerRepository practitionerRepository, ClinicRepository clinicRepository,
                                            ClinicServiceRepository serviceRepository, NotificationDispatchService dispatchService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.practitionerRepository = practitionerRepository;
        this.clinicRepository = clinicRepository;
        this.serviceRepository = serviceRepository;
        this.dispatchService = dispatchService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentEvent(DomainAuditEvent event) {
        if (!"Appointment".equals(event.entityType())) {
            return;
        }

        NotificationEvent mapped = switch (event.action()) {
            case "APPOINTMENT_BOOK" -> null; // resolved below by current status (CONFIRMED vs PENDING_CONFIRMATION)
            case "APPOINTMENT_CONFIRM" -> NotificationEvent.APPOINTMENT_CONFIRMED;
            case "APPOINTMENT_REJECT" -> NotificationEvent.APPOINTMENT_REJECTED;
            case "APPOINTMENT_CANCEL" -> NotificationEvent.APPOINTMENT_CANCELLED;
            case "APPOINTMENT_RESCHEDULE" -> NotificationEvent.APPOINTMENT_RESCHEDULED;
            default -> null;
        };
        boolean isBookEvent = "APPOINTMENT_BOOK".equals(event.action());
        if (mapped == null && !isBookEvent) {
            return;
        }

        try {
            UUID tenantId = TenantContext.requireTenantId();
            UUID appointmentId = UUID.fromString(event.entityId());
            Appointment appointment = appointmentRepository.findByIdAndTenantIdAndDeletedFalse(appointmentId, tenantId).orElse(null);
            if (appointment == null) {
                return; // shouldn't happen post-commit, but never let a notification failure surface as an error
            }

            NotificationEvent resolvedEvent = isBookEvent
                    ? (appointment.getStatus() == Appointment.AppointmentStatus.CONFIRMED
                        ? NotificationEvent.APPOINTMENT_CONFIRMED : NotificationEvent.APPOINTMENT_REQUESTED)
                    : mapped;

            // For a reschedule, the audit entityId is the ORIGINAL appointment (see
            // AppointmentService.reschedule) - the patient should be told about the NEW time.
            Appointment forDisplay = resolvedEvent == NotificationEvent.APPOINTMENT_RESCHEDULED
                    && appointment.getReplacementAppointmentId() != null
                    ? appointmentRepository.findByIdAndTenantIdAndDeletedFalse(appointment.getReplacementAppointmentId(), tenantId).orElse(appointment)
                    : appointment;

            Patient patient = patientRepository.findByIdAndTenantIdAndDeletedFalse(appointment.getPatientId(), tenantId).orElse(null);
            if (patient == null) return;

            Map<String, String> variables = buildVariables(tenantId, forDisplay);
            dispatchService.dispatch(resolvedEvent, patient, "Appointment", forDisplay.getId().toString(), variables);
        } catch (Exception ex) {
            // Notification failure must never surface to the caller of the original
            // (already-committed) business transaction - log and move on.
            log.warn("Failed to dispatch notification for appointment event {}: {}", event, ex.getMessage(), ex);
        }
    }

    private Map<String, String> buildVariables(UUID tenantId, Appointment appointment) {
        Map<String, String> vars = new HashMap<>();
        practitionerRepository.findByIdAndTenantIdAndDeletedFalse(appointment.getPractitionerId(), tenantId)
                .ifPresent(p -> vars.put("doctorName", "Dr. " + p.getFirstName() + " " + p.getLastName()));
        clinicRepository.findByIdAndTenantIdAndDeletedFalse(appointment.getClinicId(), tenantId)
                .ifPresent(c -> {
                    vars.put("clinicName", c.getName());
                    ZoneId zone = ZoneId.of(c.getTimezone());
                    vars.put("date", DATE_FMT.format(appointment.getScheduledStart().atZone(zone)));
                    vars.put("time", TIME_FMT.format(appointment.getScheduledStart().atZone(zone)));
                });
        serviceRepository.findByIdAndTenantIdAndDeletedFalse(appointment.getServiceId(), tenantId)
                .ifPresent(s -> vars.put("serviceName", s.getName()));
        return vars;
    }
}
