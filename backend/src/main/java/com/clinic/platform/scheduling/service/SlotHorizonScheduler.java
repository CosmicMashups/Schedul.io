package com.clinic.platform.scheduling.service;

import com.clinic.platform.tenant.TenantContext;
import com.clinic.platform.tenant.domain.Tenant;
import com.clinic.platform.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Section 11: "A scheduled job can extend the horizon." Runs once daily, one tenant at a
 * time (see the concurrency note on {@link SlotGenerationService}) so today's date rolling
 * forward keeps every practitioner's slot horizon at a constant N days out without ever
 * generating years of slots up front.
 *
 * TenantContext is set manually here (not via the request-scoped filter, since this runs
 * outside any HTTP request) and cleared in a finally block per tenant so one tenant's failure
 * doesn't leak context into the next iteration.
 */
@Component
public class SlotHorizonScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlotHorizonScheduler.class);

    private final TenantRepository tenantRepository;
    private final SlotGenerationService slotGenerationService;

    public SlotHorizonScheduler(TenantRepository tenantRepository, SlotGenerationService slotGenerationService) {
        this.tenantRepository = tenantRepository;
        this.slotGenerationService = slotGenerationService;
    }

    @Scheduled(cron = "${clinic.scheduling.horizon-job-cron:0 0 1 * * *}") // default: 1am daily
    public void extendHorizonForAllTenants() {
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
                continue;
            }
            try {
                TenantContext.setTenantId(tenant.getId());
                slotGenerationService.generateForTenant(tenant.getId());
            } catch (Exception ex) {
                log.error("Slot horizon extension failed for tenant {}: {}", tenant.getId(), ex.getMessage(), ex);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
