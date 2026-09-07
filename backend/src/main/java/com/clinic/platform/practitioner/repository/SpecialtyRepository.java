package com.clinic.platform.practitioner.repository;

import com.clinic.platform.practitioner.domain.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpecialtyRepository extends JpaRepository<Specialty, UUID> {
    List<Specialty> findByTenantIdAndDeletedFalse(UUID tenantId);
}
