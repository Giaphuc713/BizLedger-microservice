SELECT 'CREATE DATABASE identity_db'
    WHERE NOT EXISTS (
        SELECT
        FROM pg_database
        WHERE datname = 'identity_db'
    ) \gexec
SELECT 'CREATE DATABASE catalog_db'
    WHERE NOT EXISTS (
        SELECT
        FROM pg_database
        WHERE datname = 'catalog_db'
    ) \gexec
SELECT 'CREATE DATABASE inventory_db'
    WHERE NOT EXISTS (
        SELECT
        FROM pg_database
        WHERE datname = 'inventory_db'
    ) \gexec
SELECT 'CREATE DATABASE order_db'
    WHERE NOT EXISTS (
        SELECT
        FROM pg_database
        WHERE datname = 'order_db'
    ) \gexec
SELECT 'CREATE DATABASE payment_db'
    WHERE NOT EXISTS (
        SELECT
        FROM pg_database
        WHERE datname = 'payment_db'
    ) \gexec
SELECT 'CREATE DATABASE audit_db'
    WHERE NOT EXISTS (
        SELECT
        FROM pg_database
        WHERE datname = 'audit_db'
    ) \gexec
SELECT 'CREATE DATABASE notification_db'
    WHERE NOT EXISTS (
        SELECT
        FROM pg_database
        WHERE datname = 'notification_db'
    ) \gexec
SELECT 'CREATE DATABASE reporting_db'
    WHERE NOT EXISTS (
        SELECT
        FROM pg_database
        WHERE datname = 'reporting_db'
    ) \gexec
SELECT 'CREATE DATABASE job_db'
    WHERE NOT EXISTS (
        SELECT
        FROM pg_database
        WHERE datname = 'job_db'
    ) \gexec