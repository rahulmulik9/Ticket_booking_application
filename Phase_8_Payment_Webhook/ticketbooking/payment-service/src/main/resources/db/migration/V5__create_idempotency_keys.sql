CREATE TABLE idempotency_keys (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    key_value    VARCHAR(64)  NOT NULL,
    request_hash VARCHAR(64)  NOT NULL,   -- SHA-256 of the request body
    resource_id  BIGINT,                  -- the booking id (payment id in Payment service)
    expires_at   TIMESTAMP    NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    -- the last defence: the database lets only one request claim a key
    CONSTRAINT uq_idempotency_user_key UNIQUE (user_id, key_value)
);

CREATE INDEX idx_idempotency_expires_at ON idempotency_keys (expires_at);