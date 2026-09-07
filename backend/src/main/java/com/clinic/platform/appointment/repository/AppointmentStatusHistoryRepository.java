package com.clinic.platform.appointment.repository;

import com.clinic.platform.appointment.domain.AppointmentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppointmentStatusHistoryRepository extends JpaRepository<AppointmentStatusHistory, UUID> {
    List<AppointmentStatusHistory> findByTenantIdAndAppointmentIdOrderByCreatedAtAsc(UUID tenantId, UUID appointmentId);
}
