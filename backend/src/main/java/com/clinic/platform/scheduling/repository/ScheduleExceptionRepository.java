package com.clinic.platform.scheduling.repository;

import com.clinic.platform.scheduling.domain.ScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, UUID> {

    List<ScheduleException> findByTenantIdAndPractitionerIdAndExceptionDateBetween(
            UUID tenantId, UUID practitionerId, LocalDate from, LocalDate to);

    /** Section 27: find every exception recorded for a practitioner on/after a date, e.g. when they go on leave. */
    List<ScheduleException> findByTenantIdAndPractitionerIdAndExceptionDateGreaterThanEqual(
            UUID tenantId, UUID practitionerId, LocalDate fromDate);
}
