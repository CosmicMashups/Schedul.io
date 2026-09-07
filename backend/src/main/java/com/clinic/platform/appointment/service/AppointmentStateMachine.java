package com.clinic.platform.appointment.service;

import com.clinic.platform.appointment.domain.Appointment.AppointmentStatus;
import com.clinic.platform.common.exception.ApiException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.clinic.platform.appointment.domain.Appointment.AppointmentStatus.*;

/**
 * Section 14: "Do not use random boolean fields... Use a state machine." This is the single
 * source of truth for which transitions are legal; every service method that changes
 * Appointment.status MUST go through {@link #assertTransition} rather than calling
 * appointment.setStatus() directly, or this table stops being authoritative.
 *
 * CHECKED_IN/IN_QUEUE/IN_CONSULTATION/COMPLETED transitions are declared here but not
 * triggered by any service yet — that's Milestones 6/7 (check-in/queue/visit). They're
 * included now so this table doesn't need revisiting when those modules land.
 */
final class AppointmentStateMachine {

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> TRANSITIONS = new EnumMap<>(AppointmentStatus.class);

    static {
        TRANSITIONS.put(REQUESTED, EnumSet.of(PENDING_CONFIRMATION, CONFIRMED, REJECTED, CANCELLED));
        TRANSITIONS.put(PENDING_CONFIRMATION, EnumSet.of(CONFIRMED, REJECTED, CANCELLED));
        TRANSITIONS.put(CONFIRMED, EnumSet.of(CHECKED_IN, CANCELLED, RESCHEDULED, NO_SHOW));
        TRANSITIONS.put(CHECKED_IN, EnumSet.of(IN_QUEUE, LEFT_WITHOUT_BEING_SEEN));
        TRANSITIONS.put(IN_QUEUE, EnumSet.of(IN_CONSULTATION, LEFT_WITHOUT_BEING_SEEN));
        TRANSITIONS.put(IN_CONSULTATION, EnumSet.of(COMPLETED));
        // Terminal states: REJECTED, CANCELLED, RESCHEDULED, NO_SHOW, LEFT_WITHOUT_BEING_SEEN, COMPLETED
    }

    private AppointmentStateMachine() {
    }

    static void assertTransition(AppointmentStatus from, AppointmentStatus to) {
        Set<AppointmentStatus> allowed = TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw ApiException.conflict("INVALID_APPOINTMENT_TRANSITION",
                    "Cannot transition appointment from " + from + " to " + to + ".");
        }
    }
}
