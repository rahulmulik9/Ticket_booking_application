-- ============ Cinema ============
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
    version     BIGINT      NOT NULL DEFAULT 0,        -- optimistic locking (Phase 4)
    CONSTRAINT uq_seat_per_show UNIQUE (show_id, seat_number)
);

-- ============ Users (Phase 5) ============
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- ============ Bookings ============
CREATE TABLE bookings (
    id           BIGSERIAL PRIMARY KEY,
    show_id      BIGINT         NOT NULL REFERENCES shows (id),
    user_id      BIGINT         NOT NULL REFERENCES users (id),
    total_amount NUMERIC(10, 2) NOT NULL,
    status       VARCHAR(20)    NOT NULL,
    created_at   TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE TABLE booking_seats (
    id         BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings (id),
    seat_id    BIGINT NOT NULL REFERENCES seats (id)
);

-- ============ Indexes ============
-- Postgres does not index foreign keys automatically, so each lookup path gets one.

-- movie name search: matches the lower(title) LIKE lower('%...%') query Spring Data generates (Phase 3)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_movies_title_trgm ON movies USING gin (lower(title) gin_trgm_ops);

-- shows of a movie (Phase 3)
CREATE INDEX idx_shows_movie_id ON shows (movie_id);

-- "my bookings" (Phase 5)
CREATE INDEX idx_bookings_user_id ON bookings (user_id);

-- seats of a booking, used when reading and cancelling a booking
CREATE INDEX idx_booking_seats_booking_id ON booking_seats (booking_id);