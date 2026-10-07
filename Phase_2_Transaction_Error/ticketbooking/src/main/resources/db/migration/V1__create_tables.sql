CREATE TABLE movies (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    language         VARCHAR(50)  NOT NULL,
    duration_minutes INT          NOT NULL
);

CREATE TABLE shows (
    id         BIGSERIAL PRIMARY KEY,
    movie_id   BIGINT         NOT NULL REFERENCES movies (id),
    start_time TIMESTAMP      NOT NULL,
    price      NUMERIC(10, 2) NOT NULL
);

CREATE TABLE seats (
    id          BIGSERIAL PRIMARY KEY,
    show_id     BIGINT      NOT NULL REFERENCES shows (id),
    seat_number VARCHAR(10) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT uq_seat_per_show UNIQUE (show_id, seat_number)
);

CREATE TABLE bookings (
    id           BIGSERIAL PRIMARY KEY,
    show_id      BIGINT         NOT NULL REFERENCES shows (id),
    user_id      BIGINT         NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    status       VARCHAR(20)    NOT NULL,
    created_at   TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE TABLE booking_seats (
    id         BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings (id),
    seat_id    BIGINT NOT NULL REFERENCES seats (id)
);