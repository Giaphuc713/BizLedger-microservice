-- =============================================================
-- Payment Service — Subscription & FX Schema (Module 13)
-- V2__add_subscriptions_and_fx.sql
-- =============================================================

CREATE TABLE subscription (
                              id                    UUID PRIMARY KEY,
                              tenant_id             UUID NOT NULL,
                              customer_id           UUID NOT NULL,
                              plan_name             VARCHAR(255) NOT NULL,
                              status                VARCHAR(50) NOT NULL, -- ACTIVE, CANCELLED, PAST_DUE
                              price                 NUMERIC(18, 4) NOT NULL,
                              currency              VARCHAR(3) NOT NULL DEFAULT 'VND',
                              billing_cycle         VARCHAR(50) NOT NULL, -- MONTHLY, YEARLY
                              started_at            TIMESTAMPTZ NOT NULL,
                              current_period_start  TIMESTAMPTZ NOT NULL,
                              current_period_end    TIMESTAMPTZ NOT NULL,
                              canceled_at           TIMESTAMPTZ,
                              created_at            TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_subscription_tenant ON subscription(tenant_id);
CREATE INDEX idx_subscription_customer ON subscription(customer_id);

-- =============================================================

CREATE TABLE fx_rate (
                         id             UUID PRIMARY KEY,
                         from_currency  VARCHAR(3) NOT NULL,
                         to_currency    VARCHAR(3) NOT NULL,
                         rate           NUMERIC(18, 6) NOT NULL,
                         updated_at     TIMESTAMPTZ NOT NULL,
                         UNIQUE (from_currency, to_currency)
);

CREATE INDEX idx_fx_rate_pairs ON fx_rate(from_currency, to_currency);
