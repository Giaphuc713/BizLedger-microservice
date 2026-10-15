-- =============================================================
-- Order Service — Database Schema (Saga Orchestrator)
-- V1__init_order.sql
-- =============================================================

CREATE TABLE orders (
    id             UUID PRIMARY KEY,
    tenant_id      UUID NOT NULL,
    customer_id    UUID,
    currency       VARCHAR(3) NOT NULL DEFAULT 'VND',
    total_amount   NUMERIC(18, 4) NOT NULL DEFAULT 0,
    status         VARCHAR(50) NOT NULL, -- DRAFT, PENDING_APPROVAL, APPROVED, PAID, APPROVAL_FAILED, CANCELLED
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ
);

CREATE INDEX idx_orders_tenant ON orders(tenant_id);
CREATE INDEX idx_orders_status ON orders(tenant_id, status);

-- =============================================================

CREATE TABLE order_items (
    id          UUID PRIMARY KEY,
    order_id    UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  UUID NOT NULL,
    sku         VARCHAR(100) NOT NULL,
    name        VARCHAR(300) NOT NULL,
    quantity    INTEGER NOT NULL,
    unit_price  NUMERIC(18, 4) NOT NULL,
    total_price NUMERIC(18, 4) NOT NULL
);

CREATE INDEX idx_order_items_order ON order_items(order_id);

-- =============================================================

CREATE TABLE order_status_history (
    id          UUID PRIMARY KEY,
    order_id    UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status   VARCHAR(50) NOT NULL,
    reason      TEXT,
    changed_at  TIMESTAMPTZ NOT NULL
);

-- =============================================================
-- Outbox Pattern
-- =============================================================
CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY,
    event_id     VARCHAR(100) NOT NULL UNIQUE,
    event_type   VARCHAR(100) NOT NULL,
    topic        VARCHAR(200) NOT NULL,
    payload      TEXT NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count  INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_ord_outbox_status ON outbox_events(status, created_at);

-- =============================================================
-- Inbox Pattern
-- =============================================================
CREATE TABLE inbox_events (
    id           BIGSERIAL PRIMARY KEY,
    event_id     VARCHAR(100) NOT NULL UNIQUE,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ord_inbox_event_id ON inbox_events(event_id);
