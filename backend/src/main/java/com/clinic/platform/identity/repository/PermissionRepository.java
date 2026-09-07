package com.clinic.platform.identity.repository;

import com.clinic.platform.identity.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
}
