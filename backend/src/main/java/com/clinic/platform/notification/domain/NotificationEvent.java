package com.clinic.platform.notification.domain;

/**
 * Section 30's event list. The three APPOINTMENT_REMINDER_* values replace what was a single
 * APPOINTMENT_REMINDER value in Milestone 8 — the flagged gap was "only one tier is
 * implemented"; giving each tier its own enum value (rather than a shared event plus a
 * separate "tier" column) makes tier-aware dedupe free: ReminderScheduler's existing
 * "does a notification already exist for this event+appointment" check now works correctly
 * per tier with no schema change, since eventCode itself IS the tier.
 *
 * Wired (see AppointmentNotificationListener / QueueNotificationListener / ReminderScheduler):
 *   APPOINTMENT_REQUESTED, APPOINTMENT_CONFIRMED, APPOINTMENT_REJECTED,
 *   APPOINTMENT_RESCHEDULED, APPOINTMENT_CANCELLED, QUEUE_TURN_APPROACHING,
 *   APPOINTMENT_REMINDER_7_DAY, APPOINTMENT_REMINDER_24_HOUR, APPOINTMENT_REMINDER_2_HOUR
 *
 * Not yet wired (no trigger exists — no doctor-lateness/absence signal in the codebase):
 *   DOCTOR_RUNNING_LATE, CLINIC_CLOSED, DOCTOR_UNAVAILABLE
 */
public enum NotificationEvent {
    APPOINTMENT_REQUESTED, APPOINTMENT_CONFIRMED, APPOINTMENT_REJECTED,
    APPOINTMENT_RESCHEDULED, APPOINTMENT_CANCELLED,
    APPOINTMENT_REMINDER_7_DAY, APPOINTMENT_REMINDER_24_HOUR, APPOINTMENT_REMINDER_2_HOUR,
    DOCTOR_RUNNING_LATE, CLINIC_CLOSED, DOCTOR_UNAVAILABLE, QUEUE_TURN_APPROACHING
}
