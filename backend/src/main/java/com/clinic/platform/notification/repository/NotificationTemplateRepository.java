package com.clinic.platform.notification.repository;

import com.clinic.platform.notification.domain.NotificationChannel;
import com.clinic.platform.notification.domain.NotificationEvent;
import com.clinic.platform.notification.domain.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    /** All active templates for an event, across every configured channel — dispatch fans out to each. */
    List<NotificationTemplate> findByTenantIdAndEventCodeAndActiveTrueAndDeletedFalse(UUID tenantId, NotificationEvent eventCode);

    List<NotificationTemplate> findByTenantIdAndDeletedFalse(UUID tenantId);
}
