-- V4: Milestone 3 — scheduling engine (rules, exceptions, generated slots).

CREATE TABLE schedule_rules (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    practitioner_id         UUID NOT NULL REFERENCES practitioners(id),
    clinic_id               UUID NOT NULL REFERENCES clinics(id),
    day_of_week             VARCHAR(10) NOT NULL,
    start_time              TIME NOT NULL,
    end_time                TIME NOT NULL,
    effective_from          DATE NOT NULL,
    effective_to            DATE,
    slot_duration_minutes   INT NOT NULL,
    buffer_minutes          INT NOT NULL DEFAULT 0,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by              VARCHAR(150),
    updated_by              VARCHAR(150),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_schedule_rules_time_range CHECK (start_time < end_time)
);
CREATE INDEX idx_schedule_rules_tenant_practitioner ON schedule_rules(tenant_id, practitioner_id);
CREATE INDEX idx_schedule_rules_effective ON schedule_rules(effective_from, effective_to);

CREATE TABLE schedule_exceptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    practitioner_id     UUID NOT NULL REFERENCES practitioners(id),
    exception_date      DATE NOT NULL,
    start_time          TIME,
    end_time            TIME,
    type                VARCHAR(20) NOT NULL,
    reason              VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(150),
    updated_by          VARCHAR(150),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_schedule_exceptions_tenant_practitioner_date ON schedule_exceptions(tenant_id, practitioner_id, exception_date);

CREATE TABLE slots (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    schedule_rule_id    UUID REFERENCES schedule_rules(id),
    practitioner_id     UUID NOT NULL REFERENCES practitioners(id),
    clinic_id           UUID NOT NULL REFERENCES clinics(id),
    start_at            TIMESTAMPTZ NOT NULL,
    end_at              TIMESTAMPTZ NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'FREE',
    held_by_user_id     UUID,
    held_until          TIMESTAMPTZ,
    appointment_id      UUID, -- FK added in Milestone 4 once the appointments table exists
    overbooked          BOOLEAN NOT NULL DEFAULT FALSE,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(150),
    updated_by          VARCHAR(150),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_slots_practitioner_start UNIQUE (practitioner_id, start_at),
    CONSTRAINT chk_slots_time_range CHECK (start_at < end_at)
);
CREATE INDEX idx_slots_tenant_practitioner_start ON slots(tenant_id, practitioner_id, start_at);
CREATE INDEX idx_slots_status_held_until ON slots(status, held_until) WHERE status = 'HELD';
CREATE INDEX idx_slots_clinic_start ON slots(clinic_id, start_at);
