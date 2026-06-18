package com.gameplatform.notificationservice.consumer;

import com.gameplatform.notificationservice.domain.event.JointPurchaseParticipantsEmailRequestedEvent;
import com.gameplatform.notificationservice.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JointPurchaseParticipantsEmailRequestedConsumer {

    private final NotificationDispatchService notificationDispatchService;

    @KafkaListener(
            topics = "${app.kafka.topics.notification-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "jointPurchaseParticipantsEmailRequestedKafkaListenerContainerFactory"
    )
    public void handle(JointPurchaseParticipantsEmailRequestedEvent event, Acknowledgment acknowledgment) {
        log.info(
                "Received JointPurchaseParticipantsEmailRequestedEvent: eventId={}, offerId={}, organizerUserId={}, recipients={}",
                event.getEventId(),
                event.getOfferId(),
                event.getOrganizerUserId(),
                event.getRecipients().size()
        );

        notificationDispatchService.processJointPurchaseParticipantsEmail(event);
        acknowledgment.acknowledge();
    }
}
