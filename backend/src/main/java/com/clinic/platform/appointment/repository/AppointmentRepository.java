package com.clinic.platform.appointment.repository;

import com.clinic.platform.appointment.domain.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    List<Appointment> findByTenantIdAndPatientIdAndDeletedFalseOrderByScheduledStartDesc(UUID tenantId, UUID patientId);

    /** Section 32 staff search — filtered subset most commonly needed; extend with more params as staff UI needs them. */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.tenantId = :tenantId
          AND a.deleted = false
          AND (:practitionerId IS NULL OR a.practitionerId = :practitionerId)
          AND (:clinicId IS NULL OR a.clinicId = :clinicId)
          AND (:status IS NULL OR a.status = :status)
          AND (CAST(:fromDate AS Instant) IS NULL OR a.scheduledStart >= :fromDate)
          AND (CAST(:toDate AS Instant) IS NULL OR a.scheduledStart < :toDate)
        ORDER BY a.scheduledStart ASC
        """)
    List<Appointment> search(@Param("tenantId") UUID tenantId,
                              @Param("practitionerId") UUID practitionerId,
                              @Param("clinicId") UUID clinicId,
                              @Param("status") Appointment.AppointmentStatus status,
                              @Param("fromDate") Instant fromDate,
                              @Param("toDate") Instant toDate);
}
