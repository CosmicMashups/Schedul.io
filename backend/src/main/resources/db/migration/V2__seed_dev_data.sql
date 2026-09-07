-- V2: local/dev seed data only. In production this is replaced by a tenant-onboarding flow
-- (Phase 3 multi-tenant SaaS admin) — do not rely on this migration outside dev/test.

DO $$
DECLARE
    v_tenant_id UUID := gen_random_uuid();
    v_admin_role_id UUID := gen_random_uuid();
BEGIN
    INSERT INTO tenants (id, slug, name, status)
    VALUES (v_tenant_id, 'demo-clinic', 'Demo Clinic', 'ACTIVE');

    INSERT INTO roles (id, tenant_id, code, name, is_system_role)
    VALUES (v_admin_role_id, v_tenant_id, 'TENANT_ADMIN', 'Tenant Administrator', TRUE);

    -- Grant every seeded permission to TENANT_ADMIN for local development convenience.
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_admin_role_id, p.id FROM permissions p;

    -- password is "ChangeMe123!" (BCrypt) — rotate immediately outside local dev.
    INSERT INTO users (id, tenant_id, email, password_hash, first_name, last_name, status)
    VALUES (
        gen_random_uuid(), v_tenant_id, 'admin@demo-clinic.test',
        '$2b$10$KJAaRFpG.2Wjb2eWyxv92.k6SOz.DMj4ZiXwv3DyWktBb0/M9kZ6q',
        'Demo', 'Admin', 'ACTIVE'
    );

    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, v_admin_role_id FROM users u WHERE u.tenant_id = v_tenant_id AND u.email = 'admin@demo-clinic.test';

    RAISE NOTICE 'Seeded demo tenant % (slug=demo-clinic)', v_tenant_id;
END $$;
