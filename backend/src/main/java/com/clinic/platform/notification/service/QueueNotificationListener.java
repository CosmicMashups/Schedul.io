package com.clinic.platform.notification.service;

import com.clinic.platform.audit.service.DomainAuditEvent;
import com.clinic.platform.notification.domain.NotificationEvent;
import com.clinic.platform.patient.domain.Patient;
import com.clinic.platform.patient.repository.PatientRepository;
import com.clinic.platform.queue.domain.QueueTicket;
import com.clinic.platform.queue.repository.QueueTicketRepository;
import com.clinic.platform.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Section 31: "Instead of 'Please wait,' the patient can receive: You are #3 in the queue."
 * This milestone wires the simpler half of that idea: a QUEUE_TURN_APPROACHING notification
 * fires when a ticket is called (QUEUE_TICKET_CALL), i.e. "your turn is now" rather than a
 * live position countdown. A running position estimate ("#3 in line") would need either a
 * polling client or a push channel subscribing to queue state continuously - out of scope
 * for a request/response notification dispatch model built for one-shot events.
 */
@Component
public class QueueNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(QueueNotificationListener.class);

    private final QueueTicketRepository ticketRepository;
    private final PatientRepository patientRepository;
    private final NotificationDispatchService dispatchService;

    public QueueNotificationListener(QueueTicketRepository ticketRepository, PatientRepository patientRepository,
                                      NotificationDispatchService dispatchService) {
        this.ticketRepository = ticketRepository;
        this.patientRepository = patientRepository;
        this.dispatchService = dispatchService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQueueEvent(DomainAuditEvent event) {
        if (!"QueueTicket".equals(event.entityType()) || !"QUEUE_TICKET_CALL".equals(event.action())) {
            return;
        }

        try {
            UUID tenantId = TenantContext.requireTenantId();
            UUID ticketId = UUID.fromString(event.entityId());
            QueueTicket ticket = ticketRepository.findByIdAndTenantIdAndDeletedFalse(ticketId, tenantId).orElse(null);
            if (ticket == null) return;

            Patient patient = patientRepository.findByIdAndTenantIdAndDeletedFalse(ticket.getPatientId(), tenantId).orElse(null);
            if (patient == null) return;

            Map<String, String> vars = new HashMap<>();
            vars.put("ticketNumber", ticket.getTicketNumber());

            dispatchService.dispatch(NotificationEvent.QUEUE_TURN_APPROACHING, patient, "QueueTicket", ticket.getId().toString(), vars);
        } catch (Exception ex) {
            log.warn("Failed to dispatch queue notification for event {}: {}", event, ex.getMessage(), ex);
        }
    }
}
