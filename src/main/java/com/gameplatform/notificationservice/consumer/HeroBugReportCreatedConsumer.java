package com.gameplatform.notificationservice.consumer;

import com.gameplatform.notificationservice.domain.event.HeroBugReportCreatedEvent;
import com.gameplatform.notificationservice.service.HeroBugReportNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeroBugReportCreatedConsumer {

    private final HeroBugReportNotificationService heroBugReportNotificationService;

    @KafkaListener(
            topics = "${app.kafka.topics.hero-bug-report-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "heroBugReportCreatedKafkaListenerContainerFactory"
    )
    public void handle(HeroBugReportCreatedEvent event, Acknowledgment acknowledgment) {
        log.info(
                "Received HeroBugReportCreatedEvent: eventId={}, bugReportId={}, heroId={}, heroSlug={}",
                event.getEventId(),
                event.getBugReportId(),
                event.getHeroId(),
                event.getHeroSlug()
        );

        heroBugReportNotificationService.processHeroBugReportCreated(event);
        acknowledgment.acknowledge();
    }
}
