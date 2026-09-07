package com.clinic.platform.scheduling.service;

import com.clinic.platform.scheduling.domain.ScheduleException;
import com.clinic.platform.scheduling.domain.ScheduleRule;
import com.clinic.platform.scheduling.domain.Slot;
import com.clinic.platform.scheduling.repository.ScheduleExceptionRepository;
import com.clinic.platform.scheduling.repository.ScheduleRuleRepository;
import com.clinic.platform.scheduling.repository.SlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Section 11: "Do not pre-generate years of slots." Generates on a rolling horizon
 * (today → today + horizonDays) instead. Idempotent within a single generation pass — the
 * in-memory {@code existing} set prevents duplicate inserts for the same rule/date. The
 * DB-level unique constraint on (practitioner_id, start_at) is the final backstop, but is NOT
 * caught/retried here: a violation would abort the whole transaction (Hibernate marks the
 * persistence context rollback-only on flush failure), so this service should only ever be
 * invoked serially per tenant (the scheduled job in {@link SlotHorizonScheduler} does this
 * one tenant at a time) rather than from concurrent callers regenerating the same rule.
 *
 * Deliberately does NOT touch slots that are already HELD or BOOKED, even if a
 * ScheduleException is added afterward that would have excluded that slot — that's
 * intentional (section 27, doctor-absence handling): an already-booked appointment needs an
 * explicit reschedule/cancellation workflow (Milestone 4), not silent slot deletion.
 */
@Service
public class SlotGenerationService {

    private static final Logger log = LoggerFactory.getLogger(SlotGenerationService.class);

    private final ScheduleRuleRepository scheduleRuleRepository;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final SlotRepository slotRepository;
    private final int horizonDays;

    public SlotGenerationService(ScheduleRuleRepository scheduleRuleRepository,
                                  ScheduleExceptionRepository scheduleExceptionRepository,
                                  SlotRepository slotRepository,
                                  @Value("${clinic.scheduling.horizon-days:90}") int horizonDays) {
        this.scheduleRuleRepository = scheduleRuleRepository;
        this.scheduleExceptionRepository = scheduleExceptionRepository;
        this.slotRepository = slotRepository;
        this.horizonDays = horizonDays;
    }

    /** Regenerates the rolling horizon for a single practitioner — call this right after a ScheduleRule is created/edited. */
    @Transactional
    public int generateForPractitioner(UUID tenantId, UUID practitionerId) {
        List<ScheduleRule> rules = scheduleRuleRepository
                .findByTenantIdAndPractitionerIdAndDeletedFalseAndStatus(tenantId, practitionerId, ScheduleRule.RuleStatus.ACTIVE);
        int created = 0;
        for (ScheduleRule rule : rules) {
            created += generateFromRule(rule);
        }
        return created;
    }

    /** Called by the scheduled job (see {@link SlotHorizonScheduler}) to extend every practitioner's horizon daily. */
    @Transactional
    public int generateForTenant(UUID tenantId) {
        LocalDate horizonEnd = LocalDate.now().plusDays(horizonDays);
        List<ScheduleRule> rules = scheduleRuleRepository
                .findByTenantIdAndStatusAndEffectiveFromLessThanEqual(tenantId, ScheduleRule.RuleStatus.ACTIVE, horizonEnd);
        int created = 0;
        for (ScheduleRule rule : rules) {
            created += generateFromRule(rule);
        }
        log.info("Generated {} slots for tenant {} across {} active schedule rules (horizon={} days)",
                created, tenantId, rules.size(), horizonDays);
        return created;
    }

    private int generateFromRule(ScheduleRule rule) {
        ZoneId zone = ZoneId.of(rule.getClinic().getTimezone());
        LocalDate today = LocalDate.now(zone);
        LocalDate horizonEnd = today.plusDays(horizonDays);

        LocalDate start = rule.getEffectiveFrom().isAfter(today) ? rule.getEffectiveFrom() : today;
        LocalDate end = rule.getEffectiveTo() != null && rule.getEffectiveTo().isBefore(horizonEnd)
                ? rule.getEffectiveTo() : horizonEnd;

        int created = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (date.getDayOfWeek() != rule.getDayOfWeek()) {
                continue;
            }
            created += generateForDate(rule, date, zone);
        }
        return created;
    }

    private int generateForDate(ScheduleRule rule, LocalDate date, ZoneId zone) {
        List<ScheduleException> exceptions = scheduleExceptionRepository
                .findByTenantIdAndPractitionerIdAndExceptionDateBetween(rule.getTenantId(), rule.getPractitionerId(), date, date);

        if (exceptions.stream().anyMatch(e -> e.getStartTime() == null)) {
            return 0; // whole day blocked (LEAVE/HOLIDAY/etc. with no time range = full-day)
        }

        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        Set<Instant> existing = new HashSet<>();
        slotRepository.findByPractitionerIdAndStartAtBetween(rule.getPractitionerId(), dayStart, dayEnd)
                .forEach(s -> existing.add(s.getStartAt()));

        int stepMinutes = rule.getSlotDurationMinutes() + rule.getBufferMinutes();
        LocalTime cursor = rule.getStartTime();
        int created = 0;

        while (true) {
            LocalTime slotEnd = cursor.plusMinutes(rule.getSlotDurationMinutes());
            if (slotEnd.isAfter(rule.getEndTime())) {
                break;
            }

            LocalTime slotStart = cursor;
            boolean blocked = exceptions.stream().anyMatch(e -> overlaps(slotStart, slotEnd, e.getStartTime(), e.getEndTime()));
            Instant startAt = date.atTime(cursor).atZone(zone).toInstant();

            if (!blocked && !existing.contains(startAt)) {
                Slot slot = new Slot();
                slot.setTenantId(rule.getTenantId());
                slot.setScheduleRuleId(rule.getId());
                slot.setPractitionerId(rule.getPractitionerId());
                slot.setClinicId(rule.getClinic().getId());
                slot.setStartAt(startAt);
                slot.setEndAt(date.atTime(slotEnd).atZone(zone).toInstant());
                slot.setStatus(Slot.SlotStatus.FREE);
                slotRepository.save(slot);
                existing.add(startAt); // keep the in-memory set consistent within this same generation pass
                created++;
            }

            cursor = cursor.plusMinutes(stepMinutes);
            if (cursor.isAfter(rule.getEndTime()) || cursor.equals(rule.getEndTime())) {
                break;
            }
        }
        return created;
    }

    private boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}
