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
    version     BIGINT      NOT NULL DEFAULT 0,   -- optimistic locking
    CONSTRAINT uq_seat_per_show UNIQUE (show_id, seat_number)   -- also indexes show_id
);

-- Indexes. Postgres does not index foreign keys automatically.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_movies_title_trgm ON movies USING gin (lower(title) gin_trgm_ops);   -- name search
CREATE INDEX idx_shows_movie_id ON shows (movie_id);                                   -- shows of a movie