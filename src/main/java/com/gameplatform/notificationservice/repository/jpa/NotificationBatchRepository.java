package com.gameplatform.notificationservice.repository.jpa;

import com.gameplatform.notificationservice.domain.entity.NotificationBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationBatchRepository extends JpaRepository<NotificationBatch, UUID> {

    Optional<NotificationBatch> findByEventId(UUID eventId);
}
