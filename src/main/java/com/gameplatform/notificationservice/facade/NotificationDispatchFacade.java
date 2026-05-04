package com.gameplatform.notificationservice.facade;

import com.gameplatform.notificationservice.domain.event.JointPurchaseParticipantsEmailRequestedEvent;
import com.gameplatform.notificationservice.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationDispatchFacade {

    private final NotificationDispatchService notificationDispatchService;

    public void processJointPurchaseParticipantsEmail(JointPurchaseParticipantsEmailRequestedEvent event) {
        notificationDispatchService.processJointPurchaseParticipantsEmail(event);
    }
}
