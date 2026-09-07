package com.clinic.platform.catalog.repository;

import com.clinic.platform.catalog.domain.ClinicService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicServiceRepository extends JpaRepository<ClinicService, UUID> {

    Optional<ClinicService> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    @Query("""
        SELECT DISTINCT s FROM ClinicService s
        LEFT JOIN s.clinics c
        WHERE s.tenantId = :tenantId
          AND s.deleted = false
          AND s.status = com.clinic.platform.catalog.domain.ClinicService.ServiceStatus.ACTIVE
          AND (:clinicId IS NULL OR c.id = :clinicId)
        """)
    List<ClinicService> findActive(@Param("tenantId") UUID tenantId, @Param("clinicId") UUID clinicId);
}
