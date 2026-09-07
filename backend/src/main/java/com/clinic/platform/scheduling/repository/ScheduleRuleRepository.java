package com.clinic.platform.scheduling.repository;

import com.clinic.platform.scheduling.domain.ScheduleRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleRuleRepository extends JpaRepository<ScheduleRule, UUID> {

    Optional<ScheduleRule> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    List<ScheduleRule> findByTenantIdAndPractitionerIdAndDeletedFalseAndStatus(
            UUID tenantId, UUID practitionerId, ScheduleRule.RuleStatus status);

    /** All active rules whose effective window overlaps the given generation horizon — used by the rolling-horizon job. */
    List<ScheduleRule> findByTenantIdAndStatusAndEffectiveFromLessThanEqual(
            UUID tenantId, ScheduleRule.RuleStatus status, LocalDate horizonEnd);
}
