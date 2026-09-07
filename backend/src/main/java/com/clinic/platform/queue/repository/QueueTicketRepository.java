package com.clinic.platform.queue.repository;

import com.clinic.platform.queue.domain.QueueTicket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueTicketRepository extends JpaRepository<QueueTicket, UUID> {

    Optional<QueueTicket> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    /** Pessimistic lock for call-next/skip/cancel — same rationale as SlotRepository.lockForUpdate (section 13's pattern applied to queue ops). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM QueueTicket t WHERE t.id = :id AND t.tenantId = :tenantId")
    Optional<QueueTicket> lockForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    /** Section 24's doctor queue board / section 25's reception dashboard: today's active tickets for a queue, priority-ordered. */
    @Query("""
        SELECT t FROM QueueTicket t
        WHERE t.tenantId = :tenantId AND t.queueId = :queueId
          AND t.status IN (com.clinic.platform.queue.domain.QueueTicket.TicketStatus.WAITING,
                            com.clinic.platform.queue.domain.QueueTicket.TicketStatus.CALLED)
        ORDER BY t.priority ASC, t.createdAt ASC
        """)
    List<QueueTicket> findActiveByQueueOrderedByPriority(@Param("tenantId") UUID tenantId, @Param("queueId") UUID queueId);

    /** Used to generate the next sequential ticket number ("A-023") for a queue on a given day. */
    long countByTenantIdAndQueueIdAndCreatedAtBetween(UUID tenantId, UUID queueId, Instant dayStart, Instant dayEnd);

    /** Section 46 reporting: completed tickets in a date range, for average-wait-time computation. */
    List<QueueTicket> findByTenantIdAndStatusAndCreatedAtBetween(UUID tenantId, QueueTicket.TicketStatus status, Instant from, Instant to);
}
