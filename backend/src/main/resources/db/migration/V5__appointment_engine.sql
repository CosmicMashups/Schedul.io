-- V5: Milestone 4 — appointment types, appointments, status history.

CREATE TABLE appointment_types (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                   UUID NOT NULL REFERENCES tenants(id),
    code                        VARCHAR(100) NOT NULL,
    name                        VARCHAR(150) NOT NULL,
    confirmation_policy         VARCHAR(30) NOT NULL DEFAULT 'INSTANT_CONFIRMATION',
    cancellation_window_hours   INT,
    advance_booking_limit_days  INT,
    active                      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                  VARCHAR(150),
    updated_by                  VARCHAR(150),
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_appointment_types_tenant_code UNIQUE (tenant_id, code)
);
CREATE INDEX idx_appointment_types_tenant ON appointment_types(tenant_id);

CREATE TABLE appointments (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                   UUID NOT NULL REFERENCES tenants(id),
    patient_id                  UUID NOT NULL REFERENCES patients(id),
    practitioner_id             UUID NOT NULL REFERENCES practitioners(id),
    clinic_id                   UUID NOT NULL REFERENCES clinics(id),
    service_id                  UUID NOT NULL REFERENCES services(id),
    appointment_type_id         UUID NOT NULL REFERENCES appointment_types(id),
    slot_id                     UUID NOT NULL REFERENCES slots(id),
    scheduled_start              TIMESTAMPTZ NOT NULL,
    scheduled_end                TIMESTAMPTZ NOT NULL,
    status                      VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    reason                      VARCHAR(500),
    source                      VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    previous_appointment_id     UUID,
    replacement_appointment_id  UUID,
    cancelled_by                VARCHAR(150),
    cancellation_reason         VARCHAR(30),
    cancellation_note           VARCHAR(500),
    cancelled_at                TIMESTAMPTZ,
    confirmed_at                TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                  VARCHAR(150),
    updated_by                  VARCHAR(150),
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_appointments_tenant_patient ON appointments(tenant_id, patient_id);
CREATE INDEX idx_appointments_tenant_practitioner_start ON appointments(tenant_id, practitioner_id, scheduled_start);
CREATE INDEX idx_appointments_tenant_status ON appointments(tenant_id, status);

-- Reschedule chain self-references, added after the table exists.
ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_previous FOREIGN KEY (previous_appointment_id) REFERENCES appointments(id),
    ADD CONSTRAINT fk_appointments_replacement FOREIGN KEY (replacement_appointment_id) REFERENCES appointments(id);

-- Now that appointments exists, give slots.appointment_id (added in V4) a real FK.
ALTER TABLE slots
    ADD CONSTRAINT fk_slots_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id);

CREATE TABLE appointment_status_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    appointment_id      UUID NOT NULL REFERENCES appointments(id),
    from_status         VARCHAR(30),
    to_status           VARCHAR(30) NOT NULL,
    reason              VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(150),
    updated_by          VARCHAR(150),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_appointment_status_history_appointment ON appointment_status_history(tenant_id, appointment_id);

-- Seed a sensible default appointment type per existing tenant so Milestone 4 is immediately
-- usable in dev without a separate admin step.
INSERT INTO appointment_types (tenant_id, code, name, confirmation_policy)
SELECT id, 'GENERAL', 'General Consultation', 'INSTANT_CONFIRMATION' FROM tenants;
