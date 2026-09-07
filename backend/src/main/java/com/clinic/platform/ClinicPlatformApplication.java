package com.clinic.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clinic Access Platform - Milestone 1: Foundation.
 * Modular monolith. See package structure:
 *  - tenant    : multi-tenant resolution (tenant_id / clinic_id context)
 *  - identity  : users, roles, permissions, auth
 *  - security  : JWT + Spring Security wiring
 *  - audit     : append-only audit event logging
 *  - common    : cross-cutting base entities, exceptions, API envelope
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableScheduling // powers SlotHorizonScheduler (Milestone 3) and future reminder/expiry jobs
public class ClinicPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicPlatformApplication.class, args);
    }
}
