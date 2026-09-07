package com.clinic.platform.practitioner.repository;

import com.clinic.platform.practitioner.domain.Practitioner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PractitionerRepository extends JpaRepository<Practitioner, UUID> {

    Optional<Practitioner> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<Practitioner> findByTenantIdAndUserIdAndDeletedFalse(UUID tenantId, UUID userId);

    @Query("""
        SELECT DISTINCT p FROM Practitioner p
        LEFT JOIN p.specialties s
        LEFT JOIN p.clinics c
        WHERE p.tenantId = :tenantId
          AND p.deleted = false
          AND p.status = com.clinic.platform.practitioner.domain.Practitioner.PractitionerStatus.ACTIVE
          AND (CAST(:specialtyCode AS string) IS NULL OR s.code = :specialtyCode)
          AND (:clinicId IS NULL OR c.id = :clinicId)
          AND (CAST(:nameQuery AS string) IS NULL
               OR LOWER(p.firstName || ' ' || p.lastName) LIKE LOWER(CONCAT('%', CAST(:nameQuery AS string), '%')))
        """)
    List<Practitioner> search(@Param("tenantId") UUID tenantId,
                               @Param("specialtyCode") String specialtyCode,
                               @Param("clinicId") UUID clinicId,
                               @Param("nameQuery") String nameQuery);
}
