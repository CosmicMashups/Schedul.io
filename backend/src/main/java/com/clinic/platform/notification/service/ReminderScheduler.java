package com.clinic.platform.notification.service;

import com.clinic.platform.appointment.domain.Appointment;
import com.clinic.platform.appointment.repository.AppointmentRepository;
import com.clinic.platform.clinic.repository.ClinicRepository;
import com.clinic.platform.catalog.repository.ClinicServiceRepository;
import com.clinic.platform.notification.domain.NotificationEvent;
import com.clinic.platform.notification.repository.NotificationRepository;
import com.clinic.platform.patient.domain.Patient;
import com.clinic.platform.patient.repository.PatientRepository;
import com.clinic.platform.practitioner.repository.PractitionerRepository;
import com.clinic.platform.tenant.TenantContext;
import com.clinic.platform.tenant.domain.Tenant;
import com.clinic.platform.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Closes the "only one reminder tier" gap from the previous handoff: section 30 calls for
 * 7-day / 24-hour / 2-hour reminders, and all three are now implemented as the same mechanism
 * repeated with a different lookahead window and a different {@link NotificationEvent} value.
 * Giving each tier its own enum value (rather than one event plus a separate "tier" column)
 * is what makes the existing "has a notification already been sent for this event+appointment"
 * dedupe check correct per-tier for free — see {@link NotificationEvent}'s javadoc.
 *
 * Runs hourly; each tier's window is sized to be wider than the hourly cadence so no
 * appointment is skipped between runs, without being so wide that an appointment could
 * plausibly match two tiers' windows on the same run (they don't overlap: 2h ± Xh margin
 * comfortably reaches only the 2-hour tier, similarly for 24h and 7d).
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    /** hoursBefore, event, window half-width in minutes (must be < half the gap to neighboring tiers). */
    private record Tier(long hoursBefore, NotificationEvent event, long windowMinutes) {
    }

    private static final List<Tier> TIERS = List.of(
            new Tier(7 * 24, NotificationEvent.APPOINTMENT_REMINDER_7_DAY, 90),
            new Tier(24, NotificationEvent.APPOINTMENT_REMINDER_24_HOUR, 60),
            new Tier(2, NotificationEvent.APPOINTMENT_REMINDER_2_HOUR, 30)
    );

    private final TenantRepository tenantRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final ClinicRepository clinicRepository;
    private final ClinicServiceRepository serviceRepository;
    private final NotificationDispatchService dispatchService;

    public ReminderScheduler(TenantRepository tenantRepository, AppointmentRepository appointmentRepository,
                              NotificationRepository notificationRepository, PatientRepository patientRepository,
                              PractitionerRepository practitionerRepository, ClinicRepository clinicRepository,
                              ClinicServiceRepository serviceRepository, NotificationDispatchService dispatchService) {
        this.tenantRepository = tenantRepository;
        this.appointmentRepository = appointmentRepository;
        this.notificationRepository = notificationRepository;
        this.patientRepository = patientRepository;
        this.practitionerRepository = practitionerRepository;
        this.clinicRepository = clinicRepository;
        this.serviceRepository = serviceRepository;
        this.dispatchService = dispatchService;
    }

    @Scheduled(cron = "${clinic.notification.reminder-job-cron:0 0 * * * *}") // hourly on the hour
    public void run() {
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) continue;
            try {
                TenantContext.setTenantId(tenant.getId());
                for (Tier tier : TIERS) {
                    runTierForTenant(tenant.getId(), tier);
                }
            } catch (Exception ex) {
                log.error("Reminder pass failed for tenant {}: {}", tenant.getId(), ex.getMessage(), ex);
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Transactional
    void runTierForTenant(UUID tenantId, Tier tier) {
        Instant target = Instant.now().plus(tier.hoursBefore(), ChronoUnit.HOURS);
        Instant windowStart = target.minus(tier.windowMinutes(), ChronoUnit.MINUTES);
        Instant windowEnd = target.plus(tier.windowMinutes(), ChronoUnit.MINUTES);

        List<Appointment> candidates = appointmentRepository.search(
                tenantId, null, null, Appointment.AppointmentStatus.CONFIRMED, windowStart, windowEnd);

        for (Appointment appointment : candidates) {
            boolean alreadySent = notificationRepository
                    .findByTenantIdAndRecipientPatientIdOrderByCreatedAtDesc(tenantId, appointment.getPatientId()).stream()
                    .anyMatch(n -> n.getEventCode() == tier.event() && appointment.getId().toString().equals(n.getRelatedEntityId()));
            if (alreadySent) continue;

            Patient patient = patientRepository.findByIdAndTenantIdAndDeletedFalse(appointment.getPatientId(), tenantId).orElse(null);
            if (patient == null) continue;

            Map<String, String> vars = buildVariables(tenantId, appointment);
            dispatchService.dispatch(tier.event(), patient, "Appointment", appointment.getId().toString(), vars);
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
