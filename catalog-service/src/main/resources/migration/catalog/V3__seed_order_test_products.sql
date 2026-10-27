-- Shared test tenant and products used by Order Service's gRPC product lookup.
INSERT INTO product_category (
    id, tenant_id, name, description, is_active, created_at
)
VALUES (
    'd3b07384-d113-4c92-a1b6-d3aef6d91001',
    'd3b07384-d113-4c92-a1b6-d3aef6d9ef59',
    'Test products',
    'Seed category for exercising the order flow',
    TRUE,
    CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

INSERT INTO product (
    id, tenant_id, category_id, sku, name, description, unit, price, currency, is_active, created_at
)
VALUES
    ('d3b07384-d113-4c92-a1b6-d3aef6d91011',
     'd3b07384-d113-4c92-a1b6-d3aef6d9ef59',
     'd3b07384-d113-4c92-a1b6-d3aef6d91001',
     'TEST-ORDER-001',
     'Test Order Product',
     'Seed product for exercising the order flow',
     'unit',
     125000,
     'VND',
     TRUE,
     CURRENT_TIMESTAMP),
    ('d3b07384-d113-4c92-a1b6-d3aef6d91012',
     'd3b07384-d113-4c92-a1b6-d3aef6d9ef59',
     'd3b07384-d113-4c92-a1b6-d3aef6d91001',
     'TEST-ORDER-002',
     'Second Test Order Product',
     'Another seed product for exercising multi-item orders',
     'unit',
     50000,
     'VND',
     TRUE,
     CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
