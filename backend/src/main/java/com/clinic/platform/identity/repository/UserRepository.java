package com.clinic.platform.identity.repository;

import com.clinic.platform.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    List<User> findByTenantId(UUID tenantId);
}
