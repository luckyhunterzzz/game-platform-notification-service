CREATE TABLE hero_bug_report_notification_inbox (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    bug_report_id UUID NOT NULL,
    hero_id BIGINT NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_hero_bug_report_notification_inbox_bug_report_id
    ON hero_bug_report_notification_inbox (bug_report_id);

CREATE INDEX idx_hero_bug_report_notification_inbox_status
    ON hero_bug_report_notification_inbox (status);

CREATE INDEX idx_hero_bug_report_notification_inbox_next_retry_at
    ON hero_bug_report_notification_inbox (next_retry_at);
