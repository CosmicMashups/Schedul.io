package com.clinic.platform.queue.controller;

import com.clinic.platform.common.response.ApiResponse;
import com.clinic.platform.queue.domain.Queue;
import com.clinic.platform.queue.domain.QueueTicket;
import com.clinic.platform.queue.dto.QueueResponse;
import com.clinic.platform.queue.dto.QueueTicketResponse;
import com.clinic.platform.queue.service.QueueService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAuthority('QUEUE_MANAGE')")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/queues")
    public ApiResponse<List<QueueResponse>> list(@RequestParam UUID clinicId) {
        return ApiResponse.ok(queueService.listForClinic(clinicId));
    }

    @GetMapping("/queues/{queueId}/tickets")
    public ApiResponse<List<QueueTicketResponse>> activeTickets(@PathVariable UUID queueId) {
        return ApiResponse.ok(queueService.listActiveTickets(queueId));
    }

    /** Section 25: reception can enqueue a walk-in directly, without an Appointment (section 28). */
    @PostMapping("/queues/{queueId}/walk-in")
    public ApiResponse<QueueTicketResponse> enqueueWalkIn(@PathVariable UUID queueId, @RequestBody WalkInRequest request) {
        QueueTicket ticket = queueService.enqueue(queueId, null, request.patientId(), request.practitionerId(), 4); // priority 4 = WALK_IN tier, section 23
        return ApiResponse.ok(QueueTicketResponse.from(ticket));
    }

    @PostMapping("/queues/{queueId}/call-next")
    public ApiResponse<QueueTicketResponse> callNext(@PathVariable UUID queueId) {
        return ApiResponse.ok(queueService.callNext(queueId));
    }

    @PostMapping("/queue-tickets/{id}/serve")
    public ApiResponse<QueueTicketResponse> startServing(@PathVariable UUID id) {
        return ApiResponse.ok(QueueTicketResponse.from(queueService.startServing(id)));
    }

    @PostMapping("/queue-tickets/{id}/complete")
    public ApiResponse<QueueTicketResponse> complete(@PathVariable UUID id) {
        return ApiResponse.ok(QueueTicketResponse.from(queueService.complete(id)));
    }

    @PostMapping("/queue-tickets/{id}/skip")
    public ApiResponse<QueueTicketResponse> skip(@PathVariable UUID id) {
        return ApiResponse.ok(QueueTicketResponse.from(queueService.skip(id)));
    }

    @PostMapping("/queue-tickets/{id}/cancel")
    public ApiResponse<QueueTicketResponse> cancel(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ApiResponse.ok(QueueTicketResponse.from(queueService.cancel(id, reason)));
    }

    @PostMapping("/queue-tickets/{id}/left-without-being-seen")
    public ApiResponse<QueueTicketResponse> leaveWithoutBeingSeen(@PathVariable UUID id) {
        return ApiResponse.ok(QueueTicketResponse.from(queueService.leaveWithoutBeingSeen(id)));
    }

    public record WalkInRequest(UUID patientId, UUID practitionerId) {
    }
}
