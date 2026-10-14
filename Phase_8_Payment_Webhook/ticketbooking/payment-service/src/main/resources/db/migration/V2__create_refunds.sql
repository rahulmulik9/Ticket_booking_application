CREATE TABLE refunds (
    id         BIGSERIAL PRIMARY KEY,
    payment_id BIGINT         NOT NULL REFERENCES payments (id),   -- same database, a real foreign key
    amount     NUMERIC(10, 2) NOT NULL,
    status     VARCHAR(20)    NOT NULL                              -- PENDING, DONE or FAILED
);

CREATE INDEX idx_refunds_payment_id ON refunds (payment_id);