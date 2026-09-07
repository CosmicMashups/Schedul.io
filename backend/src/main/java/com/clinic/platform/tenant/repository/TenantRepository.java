package com.clinic.platform.tenant.repository;

import com.clinic.platform.tenant.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);
    Optional<Tenant> findByIdAndStatus(UUID id, Tenant.TenantStatus status);
}
