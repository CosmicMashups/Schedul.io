-- V9: expand APPOINTMENT_REMINDER into three tiers (7-day / 24-hour / 2-hour), and add the
-- Doctor Portal permission + a seeded DOCTOR role for the demo tenant.

-- Flyway migrations are immutable once applied, so this cleans up the old single-tier
-- template (added in V7, seeded again in V8) rather than editing those files, and reseeds
-- under the new tiered event codes from the updated NotificationEvent enum.
DELETE FROM notification_templates WHERE event_code = 'APPOINTMENT_REMINDER';

INSERT INTO notification_templates (tenant_id, event_code, channel, language, body)
SELECT id, 'APPOINTMENT_REMINDER_7_DAY', 'SMS', 'en',
       'Just a heads up: you have an appointment with {{doctorName}} on {{date}} at {{time}}, at {{clinicName}}.'
FROM tenants
UNION ALL
SELECT id, 'APPOINTMENT_REMINDER_24_HOUR', 'SMS', 'en',
       'Reminder: your appointment with {{doctorName}} is tomorrow, {{date}} at {{time}}, at {{clinicName}}.'
FROM tenants
UNION ALL
SELECT id, 'APPOINTMENT_REMINDER_2_HOUR', 'SMS', 'en',
       'Your appointment with {{doctorName}} is coming up soon, at {{time}} today. See you at {{clinicName}}!'
FROM tenants;

-- ============================================================
-- Doctor Portal
-- ============================================================
INSERT INTO permissions (code, description) VALUES
    ('DOCTOR_PORTAL_ACCESS', 'Access the Doctor Portal application and resolve one''s own practitioner record');

-- Seed a DOCTOR role for the demo tenant with the permissions the Doctor Portal actually
-- exercises: viewing one's own schedule/appointments and running one's own queue.
DO $$
DECLARE
    v_tenant_id UUID;
    v_doctor_role_id UUID := gen_random_uuid();
BEGIN
    SELECT id INTO v_tenant_id FROM tenants WHERE slug = 'demo-clinic';
    IF v_tenant_id IS NOT NULL THEN
        INSERT INTO roles (id, tenant_id, code, name, is_system_role)
        VALUES (v_doctor_role_id, v_tenant_id, 'DOCTOR', 'Doctor', TRUE)
        ON CONFLICT (tenant_id, code) DO NOTHING;

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id
        FROM roles r, permissions p
        WHERE r.tenant_id = v_tenant_id AND r.code = 'DOCTOR'
          AND p.code IN ('DOCTOR_PORTAL_ACCESS', 'APPOINTMENT_READ', 'APPOINTMENT_UPDATE', 'QUEUE_MANAGE', 'CHECKIN_MANAGE')
          AND NOT EXISTS (
              SELECT 1 FROM role_permissions rp
              WHERE rp.role_id = r.id AND rp.permission_id = p.id
          );
    END IF;
END $$;
