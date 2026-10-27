-- Stock corresponding to the test products in the shared test tenant.
INSERT INTO inventory_item (
    id, tenant_id, product_id, sku, available_qty, reserved_qty, created_at
)
VALUES
    ('d3b07384-d113-4c92-a1b6-d3aef6d92011',
     'd3b07384-d113-4c92-a1b6-d3aef6d9ef59',
     'd3b07384-d113-4c92-a1b6-d3aef6d91011',
     'TEST-ORDER-001',
     100,
     0,
     CURRENT_TIMESTAMP),
    ('d3b07384-d113-4c92-a1b6-d3aef6d92012',
     'd3b07384-d113-4c92-a1b6-d3aef6d9ef59',
     'd3b07384-d113-4c92-a1b6-d3aef6d91012',
     'TEST-ORDER-002',
     100,
     0,
     CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
