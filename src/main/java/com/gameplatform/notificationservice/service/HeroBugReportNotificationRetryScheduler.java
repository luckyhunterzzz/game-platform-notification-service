package com.gameplatform.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeroBugReportNotificationRetryScheduler {

    private final HeroBugReportNotificationService heroBugReportNotificationService;

    @Value("${app.hero-bug-report.retry.enabled:true}")
    private boolean retryEnabled;

    @Scheduled(cron = "${app.hero-bug-report.retry.cron:0 */5 * * * *}")
    public void retryFailedNotifications() {
        if (!retryEnabled) {
            return;
        }

        int retriedCount = heroBugReportNotificationService.retryFailedNotifications();
        if (retriedCount > 0) {
            log.info("Retried {} failed hero bug report notification(s)", retriedCount);
        }
    }
}
