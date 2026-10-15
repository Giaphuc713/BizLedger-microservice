-- =============================================================
-- Identity Service — Seed Default Admin Account and Permissions
-- V2__seed_admin.sql
-- =============================================================

-- Seed Permissions
INSERT INTO permission (id, code, description) VALUES
                                                   ('00000000-0000-0000-0000-000000000001', 'user:read', 'Read users info'),
                                                   ('00000000-0000-0000-0000-000000000002', 'user:write', 'Write users info'),
                                                   ('00000000-0000-0000-0000-000000000003', 'role:read', 'Read role settings'),
                                                   ('00000000-0000-0000-0000-000000000004', 'role:write', 'Write role settings'),
                                                   ('00000000-0000-0000-0000-000000000005', 'product:read', 'Read product catalog'),
                                                   ('00000000-0000-0000-0000-000000000006', 'product:write', 'Write product catalog'),
                                                   ('00000000-0000-0000-0000-000000000007', 'inventory:read', 'Read inventory status'),
                                                   ('00000000-0000-0000-0000-000000000008', 'inventory:write', 'Write/adjust inventory'),
                                                   ('00000000-0000-0000-0000-000000000009', 'order:read', 'Read customer orders'),
                                                   ('00000000-0000-0000-0000-000000000010', 'order:write', 'Write customer orders'),
                                                   ('00000000-0000-0000-0000-000000000011', 'payment:read', 'Read payments/invoices'),
                                                   ('00000000-0000-0000-0000-000000000012', 'payment:write', 'Write/refund payments'),
                                                   ('00000000-0000-0000-0000-000000000013', 'audit:read', 'Read audit trail logs'),
                                                   ('00000000-0000-0000-0000-000000000014', 'compliance:read', 'Read GDPR/compliance'),
                                                   ('00000000-0000-0000-0000-000000000015', 'compliance:write', 'Write compliance setups'),
                                                   ('00000000-0000-0000-0000-000000000016', 'featureflag:read', 'Read feature flags'),
                                                   ('00000000-0000-0000-0000-000000000017', 'featureflag:write', 'Write feature flags'),
                                                   ('00000000-0000-0000-0000-000000000018', 'apikey:read', 'Read api key info'),
                                                   ('00000000-0000-0000-0000-000000000019', 'apikey:write', 'Write api key credentials')
ON CONFLICT (code) DO NOTHING;

