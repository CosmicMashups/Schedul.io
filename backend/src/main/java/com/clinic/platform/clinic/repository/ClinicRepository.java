package com.clinic.platform.clinic.repository;

import com.clinic.platform.clinic.domain.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {
    List<Clinic> findByTenantIdAndDeletedFalse(UUID tenantId);
    Optional<Clinic> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
}
