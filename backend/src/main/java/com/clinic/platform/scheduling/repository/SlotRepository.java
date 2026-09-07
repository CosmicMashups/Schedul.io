package com.clinic.platform.scheduling.repository;

import com.clinic.platform.scheduling.domain.Slot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<Slot, UUID> {

    Optional<Slot> findByPractitionerIdAndStartAt(UUID practitionerId, Instant startAt);

    /**
     * Section 13: acquires a row-level lock (SELECT ... FOR UPDATE) on the slot before the
     * caller checks its status and transitions it. Every hold/book/release operation MUST go
     * through this method — never load a Slot via findById and mutate it when the outcome
     * needs to be safe against concurrent requests for the same slot.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Slot s WHERE s.id = :id AND s.tenantId = :tenantId")
    Optional<Slot> lockForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    /** Availability query (section 40) — only FREE slots, within range, optionally filtered by clinic. */
    @Query("""
        SELECT s FROM Slot s
        WHERE s.tenantId = :tenantId
          AND s.practitionerId = :practitionerId
          AND s.status = com.clinic.platform.scheduling.domain.Slot.SlotStatus.FREE
          AND s.startAt >= :from
          AND s.startAt < :to
          AND (:clinicId IS NULL OR s.clinicId = :clinicId)
        ORDER BY s.startAt ASC
        """)
    List<Slot> findFreeSlots(@Param("tenantId") UUID tenantId,
                              @Param("practitionerId") UUID practitionerId,
                              @Param("clinicId") UUID clinicId,
                              @Param("from") Instant from,
                              @Param("to") Instant to);

    /** Used by the background sweeper that releases expired holds back to FREE (section 12). */
    List<Slot> findByStatusAndHeldUntilBefore(Slot.SlotStatus status, Instant cutoff);

    /** Used by rolling-horizon generation to avoid re-generating slots that already exist. */
    List<Slot> findByPractitionerIdAndStartAtBetween(UUID practitionerId, Instant from, Instant to);
}
