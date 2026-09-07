package com.clinic.platform.appointment.repository;

import com.clinic.platform.appointment.domain.AppointmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentTypeRepository extends JpaRepository<AppointmentType, UUID> {
    Optional<AppointmentType> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    Optional<AppointmentType> findByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);
    List<AppointmentType> findByTenantIdAndActiveTrueAndDeletedFalse(UUID tenantId);
}
