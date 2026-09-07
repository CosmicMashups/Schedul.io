-- V7: Milestone 8 - notification templates + delivery log.

CREATE TABLE notification_templates (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    event_code          VARCHAR(40) NOT NULL,
    channel             VARCHAR(20) NOT NULL,
    language            VARCHAR(10) NOT NULL DEFAULT 'en',
    body                VARCHAR(1000) NOT NULL,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(150),
    updated_by          VARCHAR(150),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_notification_templates_tenant_event_channel_lang UNIQUE (tenant_id, event_code, channel, language)
);
CREATE INDEX idx_notification_templates_tenant_event ON notification_templates(tenant_id, event_code);

CREATE TABLE notifications (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    event_code              VARCHAR(40) NOT NULL,
    channel                 VARCHAR(20) NOT NULL,
    recipient_patient_id    UUID NOT NULL REFERENCES patients(id),
    recipient_contact       VARCHAR(255),
    rendered_body           VARCHAR(1000) NOT NULL,
    status                  VARCHAR(30) NOT NULL,
    sent_at                 TIMESTAMPTZ,
    error_message           VARCHAR(500),
    related_entity_type     VARCHAR(100),
    related_entity_id       VARCHAR(100),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by              VARCHAR(150),
    updated_by              VARCHAR(150),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_notifications_tenant_patient ON notifications(tenant_id, recipient_patient_id);
CREATE INDEX idx_notifications_tenant_related ON notifications(tenant_id, related_entity_type, related_entity_id);

-- Seed a starter SMS template per event, per existing tenant, so Milestone 8 is immediately
-- observable in dev without a separate admin step (mirrors V5's GENERAL appointment type seed).
INSERT INTO notification_templates (tenant_id, event_code, channel, language, body)
SELECT id, 'APPOINTMENT_REQUESTED', 'SMS', 'en',
       'Hi! Your appointment request with {{doctorName}} for {{date}} at {{time}} has been received and is awaiting confirmation.'
FROM tenants
UNION ALL
SELECT id, 'APPOINTMENT_CONFIRMED', 'SMS', 'en',
       'Your appointment with {{doctorName}} is confirmed for {{date}} at {{time}} at {{clinicName}}.'
FROM tenants
UNION ALL
SELECT id, 'APPOINTMENT_REJECTED', 'SMS', 'en',
       'We''re sorry, your appointment request with {{doctorName}} for {{date}} could not be accommodated. Please book another time.'
FROM tenants
UNION ALL
SELECT id, 'APPOINTMENT_CANCELLED', 'SMS', 'en',
       'Your appointment with {{doctorName}} on {{date}} at {{time}} has been cancelled.'
FROM tenants
UNION ALL
SELECT id, 'APPOINTMENT_RESCHEDULED', 'SMS', 'en',
       'Your appointment with {{doctorName}} has been rescheduled to {{date}} at {{time}}.'
FROM tenants
UNION ALL
SELECT id, 'QUEUE_TURN_APPROACHING', 'SMS', 'en',
       'It''s your turn! Please proceed — your ticket number is {{ticketNumber}}.'
FROM tenants;
