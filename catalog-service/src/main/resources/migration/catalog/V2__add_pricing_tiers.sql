-- =============================================================
-- Catalog Service — Pricing Tiers Schema (Volume discount - FR-EXT-03)
-- V2__add_pricing_tiers.sql
-- =============================================================

CREATE TABLE product_pricing_tier (
                                      id             UUID PRIMARY KEY,
                                      tenant_id      UUID NOT NULL,
                                      product_id     UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
                                      min_quantity   INTEGER NOT NULL,
                                      unit_price     NUMERIC(18, 4) NOT NULL,
                                      created_at     TIMESTAMPTZ NOT NULL,
                                      updated_at     TIMESTAMPTZ,
                                      UNIQUE (product_id, min_quantity)
);

CREATE INDEX idx_product_pricing_tier_product ON product_pricing_tier(product_id);
CREATE INDEX idx_product_pricing_tier_tenant ON product_pricing_tier(tenant_id);
