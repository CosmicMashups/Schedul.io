package com.clinic.platform.scheduling.service;

import com.clinic.platform.scheduling.domain.Slot;
import com.clinic.platform.scheduling.repository.SlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Section 12: "If the patient abandons the page: HELD → FREE." Runs frequently (default every
 * 30s) since a hold is only ~5 minutes long (see SlotHoldService) — a slow sweep would keep
 * legitimately-abandoned slots unavailable to other patients for too long.
 *
 * Does not require per-tenant TenantContext: this only reads/writes slot lifecycle state
 * (no cross-tenant data is combined or returned), so a single global sweep is both simpler
 * and cheaper than iterating every tenant separately.
 */
@Component
public class SlotHoldExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlotHoldExpiryScheduler.class);

    private final SlotRepository slotRepository;

    public SlotHoldExpiryScheduler(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @Scheduled(fixedDelayString = "${clinic.scheduling.hold-sweep-interval-ms:30000}")
    @Transactional
    public void releaseExpiredHolds() {
        List<Slot> expired = slotRepository.findByStatusAndHeldUntilBefore(Slot.SlotStatus.HELD, Instant.now());
        if (expired.isEmpty()) {
            return;
        }
        for (Slot slot : expired) {
            slot.setStatus(Slot.SlotStatus.FREE);
            slot.setHeldByUserId(null);
            slot.setHeldUntil(null);
        }
        slotRepository.saveAll(expired);
        log.info("Released {} expired slot holds back to FREE", expired.size());
    }
}
