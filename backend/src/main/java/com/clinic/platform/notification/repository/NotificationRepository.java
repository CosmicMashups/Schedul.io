package com.clinic.platform.notification.repository;

import com.clinic.platform.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByTenantIdAndRecipientPatientIdOrderByCreatedAtDesc(UUID tenantId, UUID recipientPatientId);
}
