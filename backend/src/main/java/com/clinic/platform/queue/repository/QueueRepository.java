package com.clinic.platform.queue.repository;

import com.clinic.platform.queue.domain.Queue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueRepository extends JpaRepository<Queue, UUID> {
    Optional<Queue> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    Optional<Queue> findByTenantIdAndClinicIdAndTypeAndDeletedFalse(UUID tenantId, UUID clinicId, Queue.QueueType type);
    List<Queue> findByTenantIdAndClinicIdAndDeletedFalse(UUID tenantId, UUID clinicId);
}
