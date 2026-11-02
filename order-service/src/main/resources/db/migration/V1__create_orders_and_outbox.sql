CREATE TABLE orders (
    id              UUID PRIMARY KEY,
    user_id         VARCHAR(100)   NOT NULL,
    customer_email  VARCHAR(255)   NOT NULL,
    product_id      BIGINT         NOT NULL,
    quantity        INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price      NUMERIC(12, 2) NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    failure_reason  VARCHAR(500),
    idempotency_key VARCHAR(100)   NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL,
    version         BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT uk_orders_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_orders_user_created ON orders (user_id, created_at DESC);

CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY,
    aggregate_id VARCHAR(100) NOT NULL,
    topic        VARCHAR(100) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,
    status       VARCHAR(10)  NOT NULL,
    attempts     INTEGER      NOT NULL DEFAULT 0,
    last_error   VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL,
    sent_at      TIMESTAMPTZ
);

-- Partial index: the publisher only ever scans unsent rows
CREATE INDEX idx_outbox_new ON outbox_events (created_at) WHERE status = 'NEW';
