package com.clinic.platform.clinic.repository;

import com.clinic.platform.clinic.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    List<Room> findByTenantIdAndLocationIdAndDeletedFalse(UUID tenantId, UUID locationId);
}
