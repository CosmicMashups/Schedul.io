package com.clinic.platform.scheduling.service;

import com.clinic.platform.audit.service.DomainAuditEvent;
import com.clinic.platform.common.exception.ApiException;
import com.clinic.platform.common.util.AuditSnapshot;
import com.clinic.platform.scheduling.domain.Slot;
import com.clinic.platform.scheduling.repository.SlotRepository;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Section 12 ("Critical point: slot holding") + section 13 ("Concurrency control"). Every
 * transition here goes through {@link SlotRepository#lockForUpdate} — a pessimistic
 * SELECT ... FOR UPDATE — so that two simultaneous hold/book requests for the same slot
 * serialize at the database rather than racing in application code. This is the actual
 * enforcement point behind "100 concurrent users, same slot → 1 successful, 99 rejected"
 * (section 60).
 */
@Service
public class SlotHoldService {

    private final SlotRepository slotRepository;
    private final ApplicationEventPublisher events;
    private final AuditSnapshot auditSnapshot;
    private final long holdDurationSeconds;

    public SlotHoldService(SlotRepository slotRepository, ApplicationEventPublisher events, AuditSnapshot auditSnapshot,
                            @Value("${clinic.scheduling.hold-duration-seconds:300}") long holdDurationSeconds) {
        this.slotRepository = slotRepository;
        this.events = events;
        this.auditSnapshot = auditSnapshot;
        this.holdDurationSeconds = holdDurationSeconds;
    }

    /**
     * Section 12: "FREE → HELD for 5 minutes." Called when a patient selects a slot in the
     * booking UI, before they fill in patient details / consent / payment.
     */
    @Transactional
    public Slot hold(UUID slotId, UUID holdingUserId) {
        UUID tenantId = TenantContext.requireTenantId();
        Slot slot = slotRepository.lockForUpdate(slotId, tenantId)
                .orElseThrow(() -> ApiException.notFound("SLOT_NOT_FOUND", "Slot not found."));

        if (slot.getStatus() != Slot.SlotStatus.FREE) {
            throw ApiException.conflict("SLOT_NOT_AVAILABLE",
                    "This slot is no longer available (status: " + slot.getStatus() + ").");
        }

        String before = auditSnapshot.of(slot);
        slot.setStatus(Slot.SlotStatus.HELD);
        slot.setHeldByUserId(holdingUserId);
        slot.setHeldUntil(Instant.now().plus(holdDurationSeconds, ChronoUnit.SECONDS));
        Slot saved = slotRepository.save(slot);

        events.publishEvent(new DomainAuditEvent(
                "SLOT_HOLD", "Slot", saved.getId().toString(), before, auditSnapshot.of(saved), null));

        return saved;
    }

    /**
     * Explicit release — e.g. the patient navigates back or cancels the booking form before
     * the hold naturally expires. The background sweeper ({@link SlotHoldExpiryScheduler})
     * handles the abandonment case where this is never called.
     */
    @Transactional
    public void release(UUID slotId, UUID requestingUserId) {
        UUID tenantId = TenantContext.requireTenantId();
        Slot slot = slotRepository.lockForUpdate(slotId, tenantId)
                .orElseThrow(() -> ApiException.notFound("SLOT_NOT_FOUND", "Slot not found."));

        if (slot.getStatus() != Slot.SlotStatus.HELD) {
            return; // already released/expired/booked — releasing is idempotent, not an error
        }
        if (slot.getHeldByUserId() != null && !slot.getHeldByUserId().equals(requestingUserId)) {
            throw ApiException.forbidden("NOT_HOLD_OWNER", "This slot is held by a different user.");
        }

        String before = auditSnapshot.of(slot);
        slot.setStatus(Slot.SlotStatus.FREE);
        slot.setHeldByUserId(null);
        slot.setHeldUntil(null);
        Slot saved = slotRepository.save(slot);

        events.publishEvent(new DomainAuditEvent(
                "SLOT_RELEASE", "Slot", saved.getId().toString(), before, auditSnapshot.of(saved), null));
    }

    /**
     * Releases an already-BOOKED slot back to FREE — used by AppointmentService when an
     * appointment is cancelled or rescheduled (sections 17-18). Unlike {@link #release}, this
     * is not restricted to the original holder: cancellation/rescheduling is a staff or
     * patient action on the Appointment, not the Slot, and by the time this is called the
     * caller (AppointmentService) has already authorized the action against the Appointment
     * itself.
     */
    @Transactional
    public void releaseBooked(UUID slotId, String reason) {
        UUID tenantId = TenantContext.requireTenantId();
        Slot slot = slotRepository.lockForUpdate(slotId, tenantId)
                .orElseThrow(() -> ApiException.notFound("SLOT_NOT_FOUND", "Slot not found."));

        if (slot.getStatus() != Slot.SlotStatus.BOOKED) {
            return; // nothing to release — already freed or never booked
        }

        String before = auditSnapshot.of(slot);
        slot.setStatus(Slot.SlotStatus.FREE);
        slot.setAppointmentId(null);
        slot.setOverbooked(false);
        Slot saved = slotRepository.save(slot);

        events.publishEvent(new DomainAuditEvent(
                "SLOT_RELEASE_BOOKED", "Slot", saved.getId().toString(), before, auditSnapshot.of(saved), reason));
    }

    /**
     * Converts a HELD (or FREE, for staff/phone booking that skips the hold step) slot into
     * BOOKED. Exposed here for Milestone 4 (Appointment creation) to call from within its own
     * booking transaction — it does not publish its own audit event; the caller
     * (AppointmentService) owns the audit trail for "booking", since a slot transition alone
     * isn't the full business event (an Appointment record doesn't exist until that module
     * lands).
     */
    @Transactional
    public Slot book(UUID slotId, UUID requestingUserId, UUID appointmentId, boolean allowOverbook) {
        UUID tenantId = TenantContext.requireTenantId();
        Slot slot = slotRepository.lockForUpdate(slotId, tenantId)
                .orElseThrow(() -> ApiException.notFound("SLOT_NOT_FOUND", "Slot not found."));

        boolean heldByThisUser = slot.getStatus() == Slot.SlotStatus.HELD
                && slot.getHeldByUserId() != null && slot.getHeldByUserId().equals(requestingUserId);
        boolean freeAndBookable = slot.getStatus() == Slot.SlotStatus.FREE;
        boolean overbookAllowed = allowOverbook && slot.getStatus() == Slot.SlotStatus.BOOKED;

        if (!heldByThisUser && !freeAndBookable && !overbookAllowed) {
            throw ApiException.conflict("SLOT_NOT_AVAILABLE",
                    "This slot can no longer be booked (status: " + slot.getStatus() + ").");
        }

        if (overbookAllowed) {
            slot.setOverbooked(true);
        }
        slot.setStatus(Slot.SlotStatus.BOOKED);
        slot.setAppointmentId(appointmentId);
        slot.setHeldByUserId(null);
        slot.setHeldUntil(null);
        return slotRepository.save(slot);
    }
}
