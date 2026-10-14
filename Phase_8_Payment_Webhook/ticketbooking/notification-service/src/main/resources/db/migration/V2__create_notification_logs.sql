CREATE TABLE notification_logs (
    id             BIGSERIAL PRIMARY KEY,
    booking_id     BIGINT       NOT NULL,   -- plain id, the booking lives in booking-service
    type           VARCHAR(30)  NOT NULL,   -- BOOKING_CONFIRMED, BOOKING_FAILED or BOOKING_CANCELLED
    status         VARCHAR(10)  NOT NULL,   -- SENT or FAILED
    recipient      VARCHAR(255),
    subject        VARCHAR(150),
    message        TEXT,
    failure_reason TEXT,
    created_at     TIMESTAMP    NOT NULL
);

CREATE INDEX idx_notification_logs_booking_id ON notification_logs (booking_id);