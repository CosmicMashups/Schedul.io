package com.clinic.platform.reporting.dto;

/**
 * Section 46: access metrics (appointmentsPerDay, averageBookingLeadTimeHours), appointment
 * quality metrics (confirmationRate, cancellationRate, noShowRate), and one operational
 * metric (averageWaitTimeMinutes — check-in to being called). Everything else section 46
 * lists (third-next-available appointment, doctor utilization, idle time, peak-time
 * prediction) needs either historical slot-vs-booking comparison or forecasting logic this
 * milestone doesn't build — flagged as follow-up, not silently omitted.
 */
public record ReportSummaryResponse(
        String fromDate,
        String toDate,
        long totalAppointments,
        long confirmedCount,
        long cancelledCount,
        long noShowCount,
        long completedCount,
        double confirmationRatePct,
        double cancellationRatePct,
        double noShowRatePct,
        Double averageBookingLeadTimeHours, // null if no eligible appointments in range
        Double averageWaitTimeMinutes,      // null if no completed queue tickets in range
        long queueTicketsCompleted
) {
}
