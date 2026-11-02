CREATE TABLE payments (
    id             UUID PRIMARY KEY,
    order_id       UUID           NOT NULL,
    amount         NUMERIC(12, 2) NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    failure_reason VARCHAR(500),
    created_at     TIMESTAMPTZ    NOT NULL,
    -- One payment per order: the database enforces idempotency even under a race
    CONSTRAINT uk_payments_order_id UNIQUE (order_id)
);
