-- V3: Milestone 2 master data — clinics, locations, rooms, specialties, practitioners,
-- services, patients, patient consents.

-- ============================================================
-- CLINIC
-- ============================================================
CREATE TABLE clinics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            VARCHAR(255) NOT NULL,
    address_line    VARCHAR(255) NOT NULL,
    city            VARCHAR(150),
    province        VARCHAR(150),
    postal_code     VARCHAR(20),
    contact_number  VARCHAR(30),
    timezone        VARCHAR(50) NOT NULL DEFAULT 'Asia/Manila',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(150),
    updated_by      VARCHAR(150),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_clinics_tenant ON clinics(tenant_id);

CREATE TABLE locations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    clinic_id       UUID NOT NULL REFERENCES clinics(id),
    name            VARCHAR(255) NOT NULL,
    address_line    VARCHAR(255) NOT NULL,
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(150),
    updated_by      VARCHAR(150),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_locations_tenant ON locations(tenant_id);
CREATE INDEX idx_locations_clinic ON locations(clinic_id);

CREATE TABLE rooms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    location_id     UUID NOT NULL REFERENCES locations(id),
    name            VARCHAR(150) NOT NULL,
    room_type       VARCHAR(50),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(150),
    updated_by      VARCHAR(150),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_rooms_tenant ON rooms(tenant_id);
CREATE INDEX idx_rooms_location ON rooms(location_id);

-- ============================================================
-- PRACTITIONER / SPECIALTY
-- ============================================================
CREATE TABLE specialties (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(150) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150),
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_specialties_tenant_code UNIQUE (tenant_id, code)
);
CREATE INDEX idx_specialties_tenant ON specialties(tenant_id);

CREATE TABLE practitioners (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                   UUID NOT NULL REFERENCES tenants(id),
    user_id                     UUID REFERENCES users(id),
    first_name                  VARCHAR(150) NOT NULL,
    last_name                   VARCHAR(150) NOT NULL,
    credentials                 VARCHAR(150),
    default_consultation_fee    NUMERIC(10,2),
    status                      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                  VARCHAR(150),
    updated_by                  VARCHAR(150),
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_practitioners_tenant ON practitioners(tenant_id);

CREATE TABLE practitioner_specialties (
    practitioner_id UUID NOT NULL REFERENCES practitioners(id) ON DELETE CASCADE,
    specialty_id    UUID NOT NULL REFERENCES specialties(id) ON DELETE CASCADE,
    PRIMARY KEY (practitioner_id, specialty_id)
);

CREATE TABLE practitioner_clinics (
    practitioner_id UUID NOT NULL REFERENCES practitioners(id) ON DELETE CASCADE,
    clinic_id       UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    PRIMARY KEY (practitioner_id, clinic_id)
);

-- ============================================================
-- SERVICE CATALOG
-- ============================================================
CREATE TABLE services (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    name                    VARCHAR(255) NOT NULL,
    description             VARCHAR(1000),
    duration_minutes        INT NOT NULL,
    buffer_minutes          INT NOT NULL DEFAULT 0,
    price                   NUMERIC(10,2),
    consultation_mode       VARCHAR(20) NOT NULL DEFAULT 'IN_PERSON',
    allowed_specialty_id    UUID REFERENCES specialties(id),
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by              VARCHAR(150),
    updated_by              VARCHAR(150),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_services_tenant ON services(tenant_id);

CREATE TABLE service_clinics (
    service_id  UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    clinic_id   UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    PRIMARY KEY (service_id, clinic_id)
);

-- ============================================================
-- PATIENT
-- ============================================================
CREATE TABLE patients (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                   UUID NOT NULL REFERENCES tenants(id),
    user_id                     UUID REFERENCES users(id),
    first_name                  VARCHAR(150) NOT NULL,
    middle_name                 VARCHAR(150),
    last_name                   VARCHAR(150) NOT NULL,
    suffix                      VARCHAR(20),
    birth_date                  DATE NOT NULL,
    sex                         VARCHAR(10) NOT NULL,
    mobile_number               VARCHAR(30),
    email                       VARCHAR(255),
    address_line                VARCHAR(255),
    emergency_contact_name      VARCHAR(150),
    emergency_contact_number    VARCHAR(30),
    preferred_contact_method    VARCHAR(20) NOT NULL DEFAULT 'SMS',
    registration_source         VARCHAR(20) NOT NULL DEFAULT 'FRONT_DESK',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                  VARCHAR(150),
    updated_by                  VARCHAR(150),
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_patients_tenant ON patients(tenant_id);
CREATE INDEX idx_patients_mobile ON patients(tenant_id, mobile_number);
CREATE INDEX idx_patients_email ON patients(tenant_id, email);

CREATE TABLE patient_consents (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                   UUID NOT NULL REFERENCES tenants(id),
    patient_id                  UUID NOT NULL REFERENCES patients(id),
    consent_type                VARCHAR(50) NOT NULL,
    privacy_notice_version      VARCHAR(50) NOT NULL,
    granted                     BOOLEAN NOT NULL,
    granted_at                  TIMESTAMPTZ,
    withdrawn_at                TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                  VARCHAR(150),
    updated_by                  VARCHAR(150),
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_patient_consents_tenant_patient ON patient_consents(tenant_id, patient_id);

-- ============================================================
-- New permission: a patient registering themselves via the portal (Milestone 5) needs a
-- narrower grant than staff PATIENT_WRITE — added here rather than V1 since V1/V2 may
-- already be applied in dev environments.
-- ============================================================
INSERT INTO permissions (code, description) VALUES
    ('SELF_REGISTER', 'Register one''s own patient record via the patient portal');

-- Extend the demo tenant's TENANT_ADMIN role with the new permission for local dev parity.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'TENANT_ADMIN' AND p.code = 'SELF_REGISTER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
