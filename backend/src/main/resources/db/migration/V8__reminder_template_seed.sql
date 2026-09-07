-- V8: seed the APPOINTMENT_REMINDER template (the trigger for it was added after V7).

INSERT INTO notification_templates (tenant_id, event_code, channel, language, body)
SELECT id, 'APPOINTMENT_REMINDER', 'SMS', 'en',
       'Reminder: you have an appointment with {{doctorName}} tomorrow, {{date}} at {{time}}, at {{clinicName}}.'
FROM tenants
WHERE NOT EXISTS (
    SELECT 1 FROM notification_templates t
    WHERE t.tenant_id = tenants.id AND t.event_code = 'APPOINTMENT_REMINDER' AND t.channel = 'SMS' AND t.language = 'en'
);
