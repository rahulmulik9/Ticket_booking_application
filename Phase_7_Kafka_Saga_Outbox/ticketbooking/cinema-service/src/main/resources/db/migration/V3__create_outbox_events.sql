CREATE TABLE outbox_events (
    id           BIGSERIAL PRIMARY KEY,
    topic        VARCHAR(100)  NOT NULL,
    message_key  VARCHAR(100)  NOT NULL,   -- the booking id, so one booking stays in order
    event_type   VARCHAR(100)  NOT NULL,   -- the token written to the __TypeId__ header
    payload      VARCHAR(4000) NOT NULL,   -- the event as JSON
    published    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP     NOT NULL,
    published_at TIMESTAMP
);

-- the job reads only the unpublished rows, in id order
CREATE INDEX idx_outbox_unpublished ON outbox_events (id) WHERE published = FALSE;