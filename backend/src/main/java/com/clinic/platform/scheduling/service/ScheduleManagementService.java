package com.clinic.platform.scheduling.service;

import com.clinic.platform.audit.service.DomainAuditEvent;
import com.clinic.platform.clinic.domain.Clinic;
import com.clinic.platform.clinic.repository.ClinicRepository;
import com.clinic.platform.common.exception.ApiException;
import com.clinic.platform.common.util.AuditSnapshot;
import com.clinic.platform.scheduling.domain.ScheduleException;
import com.clinic.platform.scheduling.domain.ScheduleRule;
import com.clinic.platform.scheduling.dto.ScheduleExceptionRequest;
import com.clinic.platform.scheduling.dto.ScheduleRuleRequest;
import com.clinic.platform.scheduling.dto.ScheduleRuleResponse;
import com.clinic.platform.scheduling.repository.ScheduleExceptionRepository;
import com.clinic.platform.scheduling.repository.ScheduleRuleRepository;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Section 26 (admin availability calendar) backend. Creating a rule or exception immediately
 * triggers (re)generation for that practitioner (section 9's pipeline: rules → schedule →
 * generated slots) so the effect is visible in the availability API right away, rather than
 * waiting for the nightly horizon job.
 */
@Service
public class ScheduleManagementService {

    private final ScheduleRuleRepository scheduleRuleRepository;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final ClinicRepository clinicRepository;
    private final SlotGenerationService slotGenerationService;
    private final ApplicationEventPublisher events;
    private final AuditSnapshot auditSnapshot;

    public ScheduleManagementService(ScheduleRuleRepository scheduleRuleRepository,
                                      ScheduleExceptionRepository scheduleExceptionRepository,
                                      ClinicRepository clinicRepository,
                                      SlotGenerationService slotGenerationService,
                                      ApplicationEventPublisher events,
                                      AuditSnapshot auditSnapshot) {
        this.scheduleRuleRepository = scheduleRuleRepository;
        this.scheduleExceptionRepository = scheduleExceptionRepository;
        this.clinicRepository = clinicRepository;
        this.slotGenerationService = slotGenerationService;
        this.events = events;
        this.auditSnapshot = auditSnapshot;
    }

    @Transactional
    public ScheduleRuleResponse createRule(ScheduleRuleRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Clinic clinic = clinicRepository.findByIdAndTenantIdAndDeletedFalse(request.clinicId(), tenantId)
                .orElseThrow(() -> ApiException.badRequest("INVALID_CLINIC", "Clinic does not belong to this tenant."));

        if (!request.startTime().isBefore(request.endTime())) {
            throw ApiException.badRequest("INVALID_TIME_RANGE", "startTime must be before endTime.");
        }

        ScheduleRule rule = new ScheduleRule();
        rule.setPractitionerId(request.practitionerId());
        rule.setClinic(clinic);
        rule.setDayOfWeek(request.dayOfWeek());
        rule.setStartTime(request.startTime());
        rule.setEndTime(request.endTime());
        rule.setEffectiveFrom(request.effectiveFrom());
        rule.setEffectiveTo(request.effectiveTo());
        rule.setSlotDurationMinutes(request.slotDurationMinutes());
        rule.setBufferMinutes(request.bufferMinutes());

        ScheduleRule saved = scheduleRuleRepository.save(rule);

        events.publishEvent(new DomainAuditEvent(
                "SCHEDULE_RULE_CREATE", "ScheduleRule", saved.getId().toString(), null, auditSnapshot.of(saved), null));

        int generated = slotGenerationService.generateForPractitioner(tenantId, request.practitionerId());
        events.publishEvent(new DomainAuditEvent(
                "SLOT_GENERATE", "Practitioner", request.practitionerId().toString(), null,
                "{\"generatedCount\":" + generated + "}", "triggered by new schedule rule"));

        return ScheduleRuleResponse.from(saved);
    }

    @Transactional
    public void deactivateRule(UUID ruleId) {
        UUID tenantId = TenantContext.requireTenantId();
        ScheduleRule rule = scheduleRuleRepository.findByIdAndTenantIdAndDeletedFalse(ruleId, tenantId)
                .orElseThrow(() -> ApiException.notFound("SCHEDULE_RULE_NOT_FOUND", "Schedule rule not found."));

        String before = auditSnapshot.of(rule);
        rule.setStatus(ScheduleRule.RuleStatus.INACTIVE);
        scheduleRuleRepository.save(rule);

        // NOTE: deliberately does not retroactively delete already-generated FREE slots for
        // this rule — future slot generation simply stops. Deleting future FREE slots for a
        // deactivated rule is a reasonable follow-up once a real admin UI needs it.
        events.publishEvent(new DomainAuditEvent(
                "SCHEDULE_RULE_DEACTIVATE", "ScheduleRule", ruleId.toString(), before, auditSnapshot.of(rule), null));
    }

    public List<ScheduleRuleResponse> listRulesForPractitioner(UUID practitionerId) {
        UUID tenantId = TenantContext.requireTenantId();
        return scheduleRuleRepository
                .findByTenantIdAndPractitionerIdAndDeletedFalseAndStatus(tenantId, practitionerId, ScheduleRule.RuleStatus.ACTIVE)
                .stream()
                .map(ScheduleRuleResponse::from)
                .toList();
    }

    /**
     * Section 10/27: creating an exception (leave, holiday, manual block) does NOT retract
     * already-generated FREE slots that now fall within it — see the javadoc on
     * SlotGenerationService for why. Staff must explicitly cancel/reschedule any slots (once
     * booked, via Milestone 4's appointment workflow) that fall in the blocked window; this
     * call only prevents new slots from being generated there.
     */
    @Transactional
    public void createException(ScheduleExceptionRequest request) {
        if (request.startTime() != null && request.endTime() != null && !request.startTime().isBefore(request.endTime())) {
            throw ApiException.badRequest("INVALID_TIME_RANGE", "startTime must be before endTime.");
        }

        ScheduleException exception = new ScheduleException();
        exception.setPractitionerId(request.practitionerId());
        exception.setExceptionDate(request.exceptionDate());
        exception.setStartTime(request.startTime());
        exception.setEndTime(request.endTime());
        exception.setType(request.type());
        exception.setReason(request.reason());

        ScheduleException saved = scheduleExceptionRepository.save(exception);

        events.publishEvent(new DomainAuditEvent(
                "SCHEDULE_EXCEPTION_CREATE", "ScheduleException", saved.getId().toString(), null,
                auditSnapshot.of(saved), request.reason()));
    }
}
