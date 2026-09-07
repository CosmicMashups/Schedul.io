package com.clinic.platform.clinic.repository;

import com.clinic.platform.clinic.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
    List<Location> findByTenantIdAndClinicIdAndDeletedFalse(UUID tenantId, UUID clinicId);
    Optional<Location> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
}
