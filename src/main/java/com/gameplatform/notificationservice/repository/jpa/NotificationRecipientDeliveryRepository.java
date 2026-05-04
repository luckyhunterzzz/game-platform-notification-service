package com.gameplatform.notificationservice.repository.jpa;

import com.gameplatform.notificationservice.domain.entity.NotificationRecipientDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRecipientDeliveryRepository extends JpaRepository<NotificationRecipientDelivery, UUID> {
}
