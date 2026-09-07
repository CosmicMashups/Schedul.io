-- V1: Milestone 1 foundation — tenants, identity, RBAC, audit.
-- Domain tables (patients, practitioners, services, schedules, appointments, queues, visits)
-- land in later migrations as their modules are built (Milestones 2-7).

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- TENANT
-- ============================================================
CREATE TABLE tenants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- IDENTITY / RBAC
-- ============================================================
CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    code            VARCHAR(100) NOT NULL,
    name            VARCHAR(150) NOT NULL,
    is_system_role  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(150),
    updated_by      VARCHAR(150),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_roles_tenant_code UNIQUE (tenant_id, code)
);
CREATE INDEX idx_roles_tenant ON roles(tenant_id);

CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(150) NOT NULL,
    last_name       VARCHAR(150) NOT NULL,
    mobile_number   VARCHAR(30),
    status          VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    mfa_enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(150),
    updated_by      VARCHAR(150),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email)
);
CREATE INDEX idx_users_tenant ON users(tenant_id);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ============================================================
-- AUDIT (append-only — see AuditLog entity javadoc)
-- ============================================================
CREATE TABLE audit_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    actor_user_id       VARCHAR(150),
    actor_display_name  VARCHAR(150),
    action              VARCHAR(100) NOT NULL,
    entity_type         VARCHAR(100) NOT NULL,
    entity_id           VARCHAR(100) NOT NULL,
    before_state        JSONB,
    after_state         JSONB,
    reason              VARCHAR(500),
    ip_address          VARCHAR(64),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(150),
    updated_by          VARCHAR(150),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_audit_tenant_entity ON audit_logs(tenant_id, entity_type, entity_id);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);

-- Enforce append-only at the DB level: no UPDATE or DELETE, ever, from the application role.
-- (Run this GRANT/REVOKE pairing against the actual app DB role in your environment-specific
--  migration once that role is provisioned; left as a comment here since role provisioning
--  is environment-specific, not schema-specific.)
-- REVOKE UPDATE, DELETE ON audit_logs FROM clinic_app_role;

-- ============================================================
-- SEED: permission catalog (platform-wide, not tenant-scoped)
-- ============================================================
INSERT INTO permissions (code, description) VALUES
    ('TENANT_MANAGE',        'Manage tenant-level configuration'),
    ('USER_MANAGE',          'Create, update, deactivate staff users'),
    ('ROLE_MANAGE',          'Create and assign roles/permissions'),
    ('PATIENT_READ',         'View patient identity and contact records'),
    ('PATIENT_WRITE',        'Create/update patient records'),
    ('PATIENT_READ_CLINICAL','View clinical notes tied to a patient'),
    ('PRACTITIONER_MANAGE',  'Manage doctor/staff directory records'),
    ('SERVICE_MANAGE',       'Manage the service catalog'),
    ('SCHEDULE_MANAGE',      'Manage doctor schedules and exceptions'),
    ('APPOINTMENT_CREATE',   'Book a new appointment'),
    ('APPOINTMENT_READ',     'View appointments'),
    ('APPOINTMENT_UPDATE',   'Confirm/reschedule/cancel an appointment'),
    ('CHECKIN_MANAGE',       'Check patients in for their visit'),
    ('QUEUE_MANAGE',         'Call, skip, and reorder queue tickets'),
    ('VISIT_MANAGE',         'Start/complete a consultation visit'),
    ('REPORT_VIEW',          'View operational and analytics reports'),
    ('AUDIT_VIEW',           'View the audit log');
