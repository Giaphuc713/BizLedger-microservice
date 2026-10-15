-- =============================================================
-- Inventory Service — Database Schema (Saga Participant)
-- V1__init_inventory.sql
-- =============================================================

CREATE TABLE inventory_item (
                                id             UUID PRIMARY KEY,
                                tenant_id      UUID NOT NULL,
                                product_id     UUID NOT NULL,
                                sku            VARCHAR(100) NOT NULL,
                                available_qty  INTEGER NOT NULL DEFAULT 0,
                                reserved_qty   INTEGER NOT NULL DEFAULT 0,
                                created_at     TIMESTAMPTZ NOT NULL,
                                updated_at     TIMESTAMPTZ,
                                UNIQUE(tenant_id, sku),
                                UNIQUE(tenant_id, product_id)
);

CREATE INDEX idx_inventory_tenant ON inventory_item(tenant_id);

-- =============================================================

CREATE TABLE stock_movement (
                                id             UUID PRIMARY KEY,
                                tenant_id      UUID NOT NULL,
                                product_id     UUID NOT NULL,
                                movement_type   VARCHAR(50) NOT NULL, -- "IN", "OUT", "ADJUST", "RESERVE", "RELEASE"
                                quantity       INTEGER NOT NULL,
                                reference_id   VARCHAR(100), -- orderId, transactionId
                                created_at     TIMESTAMPTZ NOT NULL
);

-- =============================================================

CREATE TABLE stock_reservation (
                                   id             UUID PRIMARY KEY,
                                   tenant_id      UUID NOT NULL,
                                   order_id       UUID NOT NULL,
                                   product_id     UUID NOT NULL,
                                   quantity       INTEGER NOT NULL,
                                   status         VARCHAR(50) NOT NULL, -- "RESERVED", "COMPLETED", "RELEASED"
                                   created_at     TIMESTAMPTZ NOT NULL,
                                   updated_at     TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_reserved_order_prod ON stock_reservation(order_id, product_id);

-- =============================================================
-- Outbox Pattern
-- =============================================================
CREATE TABLE outbox_event (
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

CREATE INDEX idx_inv_outbox_status ON outbox_event(status, created_at);

-- =============================================================
-- Inbox Pattern
-- =============================================================
CREATE TABLE inbox_event (
                             id           BIGSERIAL PRIMARY KEY,
                             event_id     VARCHAR(100) NOT NULL UNIQUE,
                             event_type   VARCHAR(100) NOT NULL,
                             processed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_inv_inbox_event_id ON inbox_event(event_id);
