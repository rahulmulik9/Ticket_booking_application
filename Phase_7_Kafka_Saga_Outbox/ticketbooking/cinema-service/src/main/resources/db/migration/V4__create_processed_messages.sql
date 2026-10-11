CREATE TABLE processed_messages (
    message_id   VARCHAR(64) PRIMARY KEY,   -- the eventId. The primary key is the last defence against a duplicate.
    processed_at TIMESTAMP   NOT NULL
);