package com.gameplatform.notificationservice.service;

import com.gameplatform.notificationservice.domain.entity.NotificationBatch;
import com.gameplatform.notificationservice.domain.entity.NotificationRecipientDelivery;
import com.gameplatform.notificationservice.domain.enums.NotificationBatchStatus;
import com.gameplatform.notificationservice.domain.enums.NotificationChannel;
import com.gameplatform.notificationservice.domain.enums.NotificationRecipientStatus;
import com.gameplatform.notificationservice.domain.event.EmailNotificationRecipientEvent;
import com.gameplatform.notificationservice.domain.event.JointPurchaseParticipantsEmailRequestedEvent;
import com.gameplatform.notificationservice.repository.jpa.NotificationBatchRepository;
import com.gameplatform.notificationservice.repository.jpa.NotificationRecipientDeliveryRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private static final String JOINT_PURCHASE_PARTICIPANTS_EMAIL_SOURCE = "JOINT_PURCHASE_PARTICIPANTS_EMAIL";

    private final NotificationBatchRepository notificationBatchRepository;
    private final NotificationRecipientDeliveryRepository notificationRecipientDeliveryRepository;
    private final JavaMailSender javaMailSender;
    private final Clock clock;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.mail.from-display-name}")
    private String mailFromDisplayName;

    public void processJointPurchaseParticipantsEmail(JointPurchaseParticipantsEmailRequestedEvent event) {
        if (notificationBatchRepository.findByEventId(event.getEventId()).isPresent()) {
            log.info("Notification batch already processed, skipping duplicate event: eventId={}", event.getEventId());
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(clock);

        NotificationBatch batch = NotificationBatch.builder()
                .id(UUID.randomUUID())
                .eventId(event.getEventId())
                .sourceType(JOINT_PURCHASE_PARTICIPANTS_EMAIL_SOURCE)
                .channel(NotificationChannel.EMAIL)
                .offerId(event.getOfferId())
                .organizerUserId(event.getOrganizerUserId())
                .organizerEmail(event.getOrganizerEmail())
                .subject(event.getSubject())
                .message(event.getMessage())
                .totalRecipients(event.getRecipients().size())
                .sentRecipients(0)
                .failedRecipients(0)
                .status(NotificationBatchStatus.PROCESSING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        notificationBatchRepository.save(batch);

        List<NotificationRecipientDelivery> deliveries = new ArrayList<>();

        for (EmailNotificationRecipientEvent recipient : event.getRecipients()) {
            deliveries.add(NotificationRecipientDelivery.builder()
                    .id(UUID.randomUUID())
                    .batchId(batch.getId())
                    .recipientUserId(recipient.getUserId())
                    .recipientEmail(recipient.getEmail())
                    .recipientName(recipient.getDisplayName())
                    .participationType(recipient.getParticipationType())
                    .status(NotificationRecipientStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }

        notificationRecipientDeliveryRepository.saveAll(deliveries);

        int sentCount = 0;
        int failedCount = 0;

        for (NotificationRecipientDelivery delivery : deliveries) {
            try {
                sendEmail(delivery.getRecipientEmail(), event.getSubject(), buildMessageBody(event, delivery));

                OffsetDateTime sentAt = OffsetDateTime.now(clock);
                delivery.setStatus(NotificationRecipientStatus.SENT);
                delivery.setSentAt(sentAt);
                delivery.setUpdatedAt(sentAt);
                delivery.setErrorMessage(null);
                sentCount++;
            } catch (MessagingException | UnsupportedEncodingException | MailException exception) {
                OffsetDateTime failedAt = OffsetDateTime.now(clock);
                delivery.setStatus(NotificationRecipientStatus.FAILED);
                delivery.setUpdatedAt(failedAt);
                delivery.setErrorMessage(truncateErrorMessage(exception.getMessage()));
                failedCount++;

                log.error(
                        "Failed to send notification email: batchId={}, deliveryId={}, recipientEmail={}",
                        batch.getId(),
                        delivery.getId(),
                        delivery.getRecipientEmail(),
                        exception
                );
            }

            notificationRecipientDeliveryRepository.save(delivery);
        }

        OffsetDateTime processedAt = OffsetDateTime.now(clock);
        batch.setSentRecipients(sentCount);
        batch.setFailedRecipients(failedCount);
        batch.setProcessedAt(processedAt);
        batch.setUpdatedAt(processedAt);
        batch.setStatus(resolveBatchStatus(sentCount, failedCount));
        notificationBatchRepository.save(batch);

        log.info(
                "Notification batch processed: batchId={}, offerId={}, sentRecipients={}, failedRecipients={}, status={}",
                batch.getId(),
                batch.getOfferId(),
                sentCount,
                failedCount,
                batch.getStatus()
        );
    }

    private void sendEmail(String recipientEmail, String subject, String messageBody)
            throws MessagingException, UnsupportedEncodingException, MailException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());

        helper.setFrom(mailFrom, mailFromDisplayName);
        helper.setTo(recipientEmail);
        helper.setSubject(subject);
        helper.setText(messageBody, false);

        javaMailSender.send(mimeMessage);
    }

    private String buildMessageBody(
            JointPurchaseParticipantsEmailRequestedEvent event,
            NotificationRecipientDelivery delivery
    ) {
        String recipientName = delivery.getRecipientName();

        if (recipientName == null || recipientName.isBlank()) {
            return event.getMessage();
        }

        return recipientName + ",\n\n" + event.getMessage();
    }

    private NotificationBatchStatus resolveBatchStatus(int sentCount, int failedCount) {
        if (failedCount == 0) {
            return NotificationBatchStatus.COMPLETED;
        }

        if (sentCount == 0) {
            return NotificationBatchStatus.FAILED;
        }

        return NotificationBatchStatus.PARTIALLY_FAILED;
    }

    private String truncateErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown mail delivery error";
        }

        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
