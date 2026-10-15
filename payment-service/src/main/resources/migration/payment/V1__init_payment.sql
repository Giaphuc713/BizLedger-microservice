-- =============================================================
-- Payment Service — Database Schema (Saga Participant)
-- V1__init_payment.sql
-- =============================================================

CREATE TABLE invoice (
                         id             UUID PRIMARY KEY,
                         tenant_id      UUID NOT NULL,
                         order_id       UUID NOT NULL,
                         amount         NUMERIC(18, 4) NOT NULL DEFAULT 0,
                         currency       VARCHAR(3) NOT NULL DEFAULT 'VND',
                         status         VARCHAR(50) NOT NULL, -- PENDING_PAYMENT, PAID, CANCELLED
                         created_at     TIMESTAMPTZ NOT NULL,
                         updated_at     TIMESTAMPTZ,
                         UNIQUE(tenant_id, order_id)
);

CREATE INDEX idx_invoice_tenant ON invoice(tenant_id);

-- =============================================================

CREATE TABLE payment_transaction (
                                     id             UUID PRIMARY KEY,
                                     tenant_id      UUID NOT NULL,
                                     invoice_id     UUID NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
                                     amount         NUMERIC(18, 4) NOT NULL,
                                     payment_method VARCHAR(50) NOT NULL, -- BANK_TRANSFER, MOMO, CREDIT_CARD
                                     reference_no   VARCHAR(100) NOT NULL UNIQUE,
                                     status         VARCHAR(50) NOT NULL, -- SUCCESS, FAILED
                                     processed_at   TIMESTAMPTZ NOT NULL
);

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

CREATE INDEX idx_pay_outbox_status ON outbox_event(status, created_at);

-- =============================================================
-- Inbox Pattern
-- =============================================================
CREATE TABLE inbox_event (
                             id           BIGSERIAL PRIMARY KEY,
                             event_id     VARCHAR(100) NOT NULL UNIQUE,
                             event_type   VARCHAR(100) NOT NULL,
                             processed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_pay_inbox_event_id ON inbox_event(event_id);
