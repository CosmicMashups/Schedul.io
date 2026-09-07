package com.clinic.platform.appointment.controller;

import com.clinic.platform.appointment.domain.Appointment;
import com.clinic.platform.appointment.dto.*;
import com.clinic.platform.appointment.service.AppointmentService;
import com.clinic.platform.common.response.ApiResponse;
import com.clinic.platform.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // ============================================================
    // Patient-facing (section 39's "Patient API")
    // ============================================================

    @PostMapping("/api/v1/appointments")
    @PreAuthorize("hasAnyAuthority('APPOINTMENT_CREATE', 'SELF_REGISTER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AppointmentResponse> book(@Valid @RequestBody BookAppointmentRequest request) {
        return ApiResponse.ok(appointmentService.book(CurrentUser.require(), request));
    }

    @GetMapping("/api/v1/appointments/{id}")
    public ApiResponse<AppointmentResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(appointmentService.get(id));
    }

    @GetMapping("/api/v1/patients/{patientId}/appointments")
    public ApiResponse<List<AppointmentResponse>> listForPatient(@PathVariable UUID patientId) {
        return ApiResponse.ok(appointmentService.listForPatient(patientId));
    }

    @PostMapping("/api/v1/appointments/{id}/cancel")
    public ApiResponse<AppointmentResponse> cancel(@PathVariable UUID id, @Valid @RequestBody CancelAppointmentRequest request) {
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.ok(appointmentService.cancel(id, actor, request));
    }

    @PostMapping("/api/v1/appointments/{id}/reschedule")
    public ApiResponse<AppointmentResponse> reschedule(@PathVariable UUID id, @Valid @RequestBody RescheduleAppointmentRequest request) {
        return ApiResponse.ok(appointmentService.reschedule(CurrentUser.require(), id, request));
    }

    // ============================================================
    // Staff-facing (section 39's "Staff API")
    // ============================================================

    @PostMapping("/api/v1/staff/appointments")
    @PreAuthorize("hasAuthority('APPOINTMENT_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AppointmentResponse> staffBook(@Valid @RequestBody BookAppointmentRequest request) {
        // Section 29: phone/front-desk booking reuses the exact same booking pipeline — the
        // only difference is the staff user performs the transaction and typically passes
        // source=PHONE or FRONT_DESK in the request body.
        return ApiResponse.ok(appointmentService.book(CurrentUser.require(), request));
    }

    @GetMapping("/api/v1/staff/appointments")
    @PreAuthorize("hasAuthority('APPOINTMENT_READ')")
    public ApiResponse<List<AppointmentResponse>> search(
            @RequestParam(required = false) UUID practitionerId,
            @RequestParam(required = false) UUID clinicId,
            @RequestParam(required = false) Appointment.AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {
        return ApiResponse.ok(appointmentService.search(practitionerId, clinicId, status, fromDate, toDate));
    }

    @PostMapping("/api/v1/staff/appointments/{id}/confirm")
    @PreAuthorize("hasAuthority('APPOINTMENT_UPDATE')")
    public ApiResponse<AppointmentResponse> confirm(@PathVariable UUID id) {
        return ApiResponse.ok(appointmentService.confirm(id));
    }

    @PostMapping("/api/v1/staff/appointments/{id}/reject")
    @PreAuthorize("hasAuthority('APPOINTMENT_UPDATE')")
    public ApiResponse<AppointmentResponse> reject(@PathVariable UUID id, @RequestBody(required = false) RejectAppointmentRequest request) {
        return ApiResponse.ok(appointmentService.reject(id, request != null ? request : new RejectAppointmentRequest(null)));
    }

    @PostMapping("/api/v1/staff/appointments/{id}/no-show")
    @PreAuthorize("hasAuthority('APPOINTMENT_UPDATE')")
    public ApiResponse<AppointmentResponse> markNoShow(@PathVariable UUID id) {
        return ApiResponse.ok(appointmentService.markNoShow(id));
    }
}
