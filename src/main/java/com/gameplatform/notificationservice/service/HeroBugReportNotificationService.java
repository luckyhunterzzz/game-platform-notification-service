package com.gameplatform.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameplatform.notificationservice.domain.entity.HeroBugReportNotificationInbox;
import com.gameplatform.notificationservice.domain.event.HeroBugReportCreatedEvent;
import com.gameplatform.notificationservice.repository.jpa.HeroBugReportNotificationInboxRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeroBugReportNotificationService {

    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final DateTimeFormatter CREATED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");

    private final HeroBugReportNotificationInboxRepository heroBugReportNotificationInboxRepository;
    private final ObjectMapper objectMapper;
    private final JavaMailSender javaMailSender;
    private final Clock clock;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.mail.from-display-name}")
    private String mailFromDisplayName;

    @Value("${app.mail.admin-recipient}")
    private String adminRecipient;

    @Value("${app.hero-bug-report.retry.max-attempts:5}")
    private int maxRetryAttempts;

    @Value("${app.hero-bug-report.retry.batch-size:20}")
    private int retryBatchSize;

    @Value("${app.hero-bug-report.retry.delay-minutes:15}")
    private long retryDelayMinutes;

    @Transactional
    public void processHeroBugReportCreated(HeroBugReportCreatedEvent event) {
        UUID eventId = event.getEventId();
        if (eventId == null) {
            throw new IllegalArgumentException("HeroBugReportCreatedEvent.eventId must not be null");
        }

        if (heroBugReportNotificationInboxRepository.findByEventId(eventId).isPresent()) {
            log.info("Hero bug report event already exists in inbox, skipping consumer processing: eventId={}", eventId);
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        HeroBugReportNotificationInbox inboxEntry = HeroBugReportNotificationInbox.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .bugReportId(event.getBugReportId())
                .heroId(event.getHeroId())
                .payload(objectMapper.valueToTree(event))
                .status(STATUS_PROCESSING)
                .attemptCount(0)
                .createdAt(now)
                .build();

        heroBugReportNotificationInboxRepository.save(inboxEntry);
        sendAndUpdateInbox(inboxEntry, event, true);
    }

    @Transactional
    public int retryFailedNotifications() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<HeroBugReportNotificationInbox> candidates =
                heroBugReportNotificationInboxRepository
                        .findAllByStatusAndAttemptCountLessThanAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                                STATUS_FAILED,
                                maxRetryAttempts,
                                now,
                                PageRequest.of(0, retryBatchSize)
                        );

        if (candidates.isEmpty()) {
            return 0;
        }

        int processedCount = 0;

        for (HeroBugReportNotificationInbox inboxEntry : candidates) {
            HeroBugReportCreatedEvent event = objectMapper.convertValue(
                    inboxEntry.getPayload(),
                    HeroBugReportCreatedEvent.class
            );
            sendAndUpdateInbox(inboxEntry, event, false);
            processedCount++;
        }

        return processedCount;
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

    private String buildSubject(HeroBugReportCreatedEvent event) {
        String heroName = normalize(event.getHeroName());
        if (heroName != null) {
            return "Hero bug report: " + heroName;
        }

        return "Hero bug report: " + fallback(event.getHeroSlug(), "unknown-hero");
    }

    private String buildMessageBody(HeroBugReportCreatedEvent event) {
        String createdAt = event.getCreatedAt() == null
                ? "unknown"
                : CREATED_AT_FORMATTER.format(event.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC));

        return """
                A new hero bug report was created.

                Event ID: %s
                Bug report ID: %s
                Hero ID: %s
                Hero slug: %s
                Hero name: %s
                Author ID: %s
                Author name: %s
                Created at: %s

                Description:
                %s
                """.formatted(
                fallback(event.getEventId(), "unknown"),
                fallback(event.getBugReportId(), "unknown"),
                fallback(event.getHeroId(), "unknown"),
                fallback(event.getHeroSlug(), "unknown"),
                fallback(event.getHeroName(), "unknown"),
                fallback(event.getAuthorId(), "anonymous"),
                fallback(event.getAuthorName(), "anonymous"),
                createdAt,
                fallback(event.getDescription(), "")
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String fallback(Object value, String fallbackValue) {
        if (value == null) {
            return fallbackValue;
        }

        if (value instanceof String stringValue) {
            String normalized = normalize(stringValue);
            return normalized == null ? fallbackValue : normalized;
        }

        return String.valueOf(value);
    }

    private String truncateErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown mail delivery error";
        }

        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private void sendAndUpdateInbox(
            HeroBugReportNotificationInbox inboxEntry,
            HeroBugReportCreatedEvent event,
            boolean rethrowOnFailure
    ) {
        OffsetDateTime attemptStartedAt = OffsetDateTime.now(clock);
        inboxEntry.setStatus(STATUS_PROCESSING);
        inboxEntry.setAttemptCount(inboxEntry.getAttemptCount() == null ? 1 : inboxEntry.getAttemptCount() + 1);
        inboxEntry.setLastAttemptAt(attemptStartedAt);
        inboxEntry.setNextRetryAt(null);
        heroBugReportNotificationInboxRepository.save(inboxEntry);

        String subject = buildSubject(event);
        String messageBody = buildMessageBody(event);

        try {
            sendEmail(adminRecipient, subject, messageBody);
            OffsetDateTime processedAt = OffsetDateTime.now(clock);
            inboxEntry.setStatus(STATUS_COMPLETED);
            inboxEntry.setErrorMessage(null);
            inboxEntry.setProcessedAt(processedAt);
            inboxEntry.setNextRetryAt(null);
            heroBugReportNotificationInboxRepository.save(inboxEntry);

            log.info(
                    "Hero bug report notification sent: eventId={}, bugReportId={}, heroId={}, recipient={}, attempt={}",
                    event.getEventId(),
                    event.getBugReportId(),
                    event.getHeroId(),
                    adminRecipient,
                    inboxEntry.getAttemptCount()
            );
        } catch (MessagingException | UnsupportedEncodingException | MailException exception) {
            OffsetDateTime failedAt = OffsetDateTime.now(clock);
            inboxEntry.setStatus(STATUS_FAILED);
            inboxEntry.setErrorMessage(truncateErrorMessage(exception.getMessage()));
            inboxEntry.setProcessedAt(failedAt);
            inboxEntry.setNextRetryAt(resolveNextRetryAt(failedAt, inboxEntry.getAttemptCount()));
            heroBugReportNotificationInboxRepository.save(inboxEntry);

            log.error(
                    "Failed to send hero bug report notification: eventId={}, bugReportId={}, heroId={}, recipient={}, attempt={}",
                    event.getEventId(),
                    event.getBugReportId(),
                    event.getHeroId(),
                    adminRecipient,
                    inboxEntry.getAttemptCount(),
                    exception
            );

            if (rethrowOnFailure) {
                throw new IllegalStateException("Failed to send hero bug report notification email", exception);
            }
        }
    }

    private OffsetDateTime resolveNextRetryAt(OffsetDateTime failedAt, int attemptCount) {
        if (attemptCount >= maxRetryAttempts) {
            return null;
        }

        return failedAt.plusMinutes(retryDelayMinutes);
    }
}
