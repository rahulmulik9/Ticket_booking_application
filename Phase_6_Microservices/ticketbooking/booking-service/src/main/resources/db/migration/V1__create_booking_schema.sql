CREATE TABLE bookings (
    id           BIGSERIAL PRIMARY KEY,
    show_id      BIGINT         NOT NULL,   -- plain id, the show lives in cinema-service
    user_id      BIGINT         NOT NULL,   -- plain id, the user lives in user-service
    total_amount NUMERIC(10, 2) NOT NULL,
    status       VARCHAR(20)    NOT NULL,
    created_at   TIMESTAMP      NOT NULL
);

CREATE TABLE booking_seats (
    id         BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings (id),   -- same database, a real foreign key
    seat_id    BIGINT NOT NULL                              -- plain id, the seat lives in cinema-service
);

CREATE INDEX idx_bookings_user_id ON bookings (user_id);
CREATE INDEX idx_booking_seats_booking_id ON booking_seats (booking_id);