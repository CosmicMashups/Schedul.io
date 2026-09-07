package com.clinic.platform.patient.repository;

import com.clinic.platform.patient.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<Patient> findByTenantIdAndUserIdAndDeletedFalse(UUID tenantId, UUID userId);

    @Query("""
        SELECT p FROM Patient p
        WHERE p.tenantId = :tenantId AND p.deleted = false
          AND (
            LOWER(p.firstName || ' ' || p.lastName) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))
            OR p.mobileNumber = :query
            OR p.email = :query
          )
        """)
    List<Patient> search(@Param("tenantId") UUID tenantId, @Param("query") String query);
}
