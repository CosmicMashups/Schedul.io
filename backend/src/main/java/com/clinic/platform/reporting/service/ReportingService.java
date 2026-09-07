package com.clinic.platform.reporting.service;

import com.clinic.platform.appointment.domain.Appointment;
import com.clinic.platform.appointment.repository.AppointmentRepository;
import com.clinic.platform.queue.domain.QueueTicket;
import com.clinic.platform.queue.repository.QueueTicketRepository;
import com.clinic.platform.reporting.dto.ReportSummaryResponse;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Section 46. Computes everything in Java over a bounded date-range query rather than SQL
 * aggregation - appropriate for the query volumes a single/small-multi-clinic deployment
 * generates (per the original architecture review's "modular monolith first" phasing); a
 * high-volume multi-tenant SaaS deployment would want these as materialized aggregates or a
 * proper OLAP-ish read model instead of scanning rows per request.
 */
@Service
public class ReportingService {

    private final AppointmentRepository appointmentRepository;
    private final QueueTicketRepository queueTicketRepository;

    public ReportingService(AppointmentRepository appointmentRepository, QueueTicketRepository queueTicketRepository) {
        this.appointmentRepository = appointmentRepository;
        this.queueTicketRepository = queueTicketRepository;
    }

    public ReportSummaryResponse summary(LocalDate from, LocalDate to, UUID clinicId, UUID practitionerId) {
        UUID tenantId = TenantContext.requireTenantId();
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Appointment> appointments = appointmentRepository.search(tenantId, practitionerId, clinicId, null, fromInstant, toInstant);

        long total = appointments.size();
        long confirmed = countStatus(appointments, Appointment.AppointmentStatus.CONFIRMED);
        long cancelled = countStatus(appointments, Appointment.AppointmentStatus.CANCELLED);
        long noShow = countStatus(appointments, Appointment.AppointmentStatus.NO_SHOW);
        long completed = countStatus(appointments, Appointment.AppointmentStatus.COMPLETED);

        // "Confirmed" for rate purposes includes anything that progressed past confirmation
        // (checked-in, queued, in consultation, completed) - not just appointments currently
        // sitting in CONFIRMED, since a completed visit was obviously confirmed at some point.
        long everConfirmedOrBeyond = appointments.stream()
                .filter(a -> a.getStatus() != Appointment.AppointmentStatus.REQUESTED
                        && a.getStatus() != Appointment.AppointmentStatus.PENDING_CONFIRMATION
                        && a.getStatus() != Appointment.AppointmentStatus.REJECTED)
                .count();

        double confirmationRate = total == 0 ? 0.0 : percentage(everConfirmedOrBeyond, total);
        double cancellationRate = total == 0 ? 0.0 : percentage(cancelled, total);
        double noShowRate = total == 0 ? 0.0 : percentage(noShow, total);

        OptionalDouble leadTimeAvg = appointments.stream()
                .filter(a -> a.getStatus() != Appointment.AppointmentStatus.REQUESTED && a.getStatus() != Appointment.AppointmentStatus.REJECTED)
                .mapToLong(a -> Duration.between(a.getCreatedAt(), a.getScheduledStart()).toMinutes())
                .average();
        Double avgLeadTimeHours = leadTimeAvg.isPresent() ? leadTimeAvg.getAsDouble() / 60.0 : null;

        List<QueueTicket> completedTickets = queueTicketRepository
                .findByTenantIdAndStatusAndCreatedAtBetween(tenantId, QueueTicket.TicketStatus.COMPLETED, fromInstant, toInstant);
        OptionalDouble waitAvg = completedTickets.stream()
                .filter(t -> t.getCalledAt() != null)
                .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getCalledAt()).toMinutes())
                .average();
        Double avgWaitMinutes = waitAvg.isPresent() ? waitAvg.getAsDouble() : null;

        return new ReportSummaryResponse(
                from.toString(), to.toString(), total, confirmed, cancelled, noShow, completed,
                confirmationRate, cancellationRate, noShowRate, avgLeadTimeHours, avgWaitMinutes, completedTickets.size()
        );
    }

    private long countStatus(List<Appointment> appointments, Appointment.AppointmentStatus status) {
        return appointments.stream().filter(a -> a.getStatus() == status).count();
    }

    private double percentage(long part, long whole) {
        return Math.round((part * 10000.0) / whole) / 100.0;
    }
}
