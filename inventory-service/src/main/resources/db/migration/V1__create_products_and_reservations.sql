CREATE TABLE products (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200)   NOT NULL,
    price           NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    -- The CHECK is the last safety net: the database itself refuses negative stock
    available_stock INTEGER        NOT NULL CHECK (available_stock >= 0),
    version         BIGINT         NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE reservations (
    order_id   UUID PRIMARY KEY,
    product_id BIGINT      NOT NULL,
    quantity   INTEGER     NOT NULL CHECK (quantity > 0),
    status     VARCHAR(20) NOT NULL,
    reason     VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_reservations_product_status ON reservations (product_id, status);
