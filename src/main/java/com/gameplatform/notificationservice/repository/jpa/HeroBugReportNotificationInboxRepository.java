package com.gameplatform.notificationservice.repository.jpa;

import com.gameplatform.notificationservice.domain.entity.HeroBugReportNotificationInbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HeroBugReportNotificationInboxRepository extends JpaRepository<HeroBugReportNotificationInbox, UUID> {

    Optional<HeroBugReportNotificationInbox> findByEventId(UUID eventId);

    List<HeroBugReportNotificationInbox> findAllByStatusAndAttemptCountLessThanAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            String status,
            Integer attemptCount,
            OffsetDateTime nextRetryAt,
            Pageable pageable
    );
}
