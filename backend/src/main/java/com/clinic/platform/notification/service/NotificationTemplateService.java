package com.clinic.platform.notification.service;

import com.clinic.platform.notification.domain.NotificationTemplate;
import com.clinic.platform.notification.dto.NotificationResponse;
import com.clinic.platform.notification.dto.NotificationTemplateRequest;
import com.clinic.platform.notification.dto.NotificationTemplateResponse;
import com.clinic.platform.notification.repository.NotificationRepository;
import com.clinic.platform.notification.repository.NotificationTemplateRepository;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationRepository notificationRepository;

    public NotificationTemplateService(NotificationTemplateRepository templateRepository, NotificationRepository notificationRepository) {
        this.templateRepository = templateRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public NotificationTemplateResponse create(NotificationTemplateRequest request) {
        NotificationTemplate template = new NotificationTemplate();
        template.setEventCode(request.eventCode());
        template.setChannel(request.channel());
        template.setLanguage(request.language() != null ? request.language() : "en");
        template.setBody(request.body());
        return NotificationTemplateResponse.from(templateRepository.save(template));
    }

    public List<NotificationTemplateResponse> list() {
        UUID tenantId = TenantContext.requireTenantId();
        return templateRepository.findByTenantIdAndDeletedFalse(tenantId).stream()
                .map(NotificationTemplateResponse::from)
                .toList();
    }

    /** Section 45-adjacent: lets staff verify what was actually sent to a patient, e.g. when investigating a missed reminder. */
    public List<NotificationResponse> historyForPatient(UUID patientId) {
        UUID tenantId = TenantContext.requireTenantId();
        return notificationRepository.findByTenantIdAndRecipientPatientIdOrderByCreatedAtDesc(tenantId, patientId).stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
