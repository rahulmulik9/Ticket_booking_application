CREATE TABLE payments (
    id         BIGSERIAL PRIMARY KEY,
    booking_id BIGINT         NOT NULL,   -- plain id, the booking lives in booking-service
    amount     NUMERIC(10, 2) NOT NULL,
    status     VARCHAR(20)    NOT NULL
);

-- one booking can have several payments (a failed try, then a success)
CREATE INDEX idx_payments_booking_id ON payments (booking_id);