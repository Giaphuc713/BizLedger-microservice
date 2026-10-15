-- =============================================================
-- Catalog Service — Database Schema
-- V1__init_catalog.sql
-- =============================================================

CREATE TABLE product_category (
                                  id          UUID PRIMARY KEY,
                                  tenant_id   UUID NOT NULL,
                                  name        VARCHAR(200) NOT NULL,
                                  description TEXT,
                                  is_active   BOOLEAN NOT NULL DEFAULT TRUE,
                                  created_at  TIMESTAMPTZ NOT NULL,
                                  updated_at  TIMESTAMPTZ
);

CREATE INDEX idx_product_category_tenant ON product_category(tenant_id);

-- =============================================================

CREATE TABLE product (
                         id             UUID PRIMARY KEY,
                         tenant_id      UUID NOT NULL,
                         category_id    UUID REFERENCES product_category(id),
                         sku            VARCHAR(100) NOT NULL,
                         name           VARCHAR(300) NOT NULL,
                         description    TEXT,
                         unit           VARCHAR(50),
                         price          NUMERIC(18, 4) NOT NULL DEFAULT 0,
                         currency       VARCHAR(3) NOT NULL DEFAULT 'VND',
                         is_active      BOOLEAN NOT NULL DEFAULT TRUE,
                         created_at     TIMESTAMPTZ NOT NULL,
                         updated_at     TIMESTAMPTZ,
                         UNIQUE(tenant_id, sku)
);

CREATE INDEX idx_product_tenant    ON product(tenant_id);
CREATE INDEX idx_product_category  ON product(category_id);
CREATE INDEX idx_product_active    ON product(tenant_id, is_active);

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

CREATE INDEX idx_outbox_status ON outbox_event(status, created_at);

-- =============================================================
-- Inbox Pattern (Idempotent Consumer)
-- =============================================================
CREATE TABLE inbox_event (
                             id           BIGSERIAL PRIMARY KEY,
                             event_id     VARCHAR(100) NOT NULL UNIQUE,
                             event_type   VARCHAR(100) NOT NULL,
                             processed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_inbox_event_id ON inbox_event(event_id);
