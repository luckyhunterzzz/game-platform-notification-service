CREATE TABLE notification_batches (
    id UUID PRIMARY KEY,
    version INTEGER NOT NULL,
    event_id UUID NOT NULL UNIQUE,
    source_type VARCHAR(100) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    offer_id UUID NOT NULL,
    organizer_user_id UUID NOT NULL,
    organizer_email VARCHAR(255),
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    total_recipients INTEGER NOT NULL,
    sent_recipients INTEGER NOT NULL,
    failed_recipients INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE notification_recipient_deliveries (
    id UUID PRIMARY KEY,
    version INTEGER NOT NULL,
    batch_id UUID NOT NULL REFERENCES notification_batches(id),
    recipient_user_id UUID,
    recipient_email VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255),
    participation_type VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    error_message VARCHAR(1000),
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_notification_batches_offer_id ON notification_batches (offer_id);
CREATE INDEX idx_notification_batches_status ON notification_batches (status);
CREATE INDEX idx_notification_recipient_deliveries_batch_id
    ON notification_recipient_deliveries (batch_id);
CREATE INDEX idx_notification_recipient_deliveries_status
    ON notification_recipient_deliveries (status);
