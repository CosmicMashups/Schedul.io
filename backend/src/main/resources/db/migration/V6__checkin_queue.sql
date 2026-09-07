-- V6: Milestone 7 - check-in and queue management.

CREATE TABLE check_ins (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    appointment_id      UUID NOT NULL REFERENCES appointments(id),
    patient_id          UUID NOT NULL REFERENCES patients(id),
    checked_in_at       TIMESTAMPTZ NOT NULL,
    method              VARCHAR(20) NOT NULL,
    identity_verified   BOOLEAN NOT NULL DEFAULT FALSE,
    checked_in_by       VARCHAR(150),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(150),
    updated_by          VARCHAR(150),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_check_ins_appointment UNIQUE (appointment_id)
);
CREATE INDEX idx_check_ins_tenant ON check_ins(tenant_id);

CREATE TABLE queues (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    clinic_id           UUID NOT NULL REFERENCES clinics(id),
    name                VARCHAR(150) NOT NULL,
    type                VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(150),
    updated_by          VARCHAR(150),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_queues_tenant_clinic_type UNIQUE (tenant_id, clinic_id, type)
);
CREATE INDEX idx_queues_tenant_clinic ON queues(tenant_id, clinic_id);

CREATE TABLE queue_tickets (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    queue_id                UUID NOT NULL REFERENCES queues(id),
    appointment_id          UUID REFERENCES appointments(id),
    patient_id              UUID NOT NULL REFERENCES patients(id),
    practitioner_id         UUID REFERENCES practitioners(id),
    ticket_number           VARCHAR(20) NOT NULL,
    priority                INT NOT NULL DEFAULT 3,
    status                  VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    called_at               TIMESTAMPTZ,
    serving_started_at      TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by              VARCHAR(150),
    updated_by              VARCHAR(150),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_queue_tickets_tenant_queue_status ON queue_tickets(tenant_id, queue_id, status);
CREATE INDEX idx_queue_tickets_tenant_queue_created ON queue_tickets(tenant_id, queue_id, created_at);
