-- Create the inventory database (orders_db is auto-created by POSTGRES_DB env var)
SELECT 'CREATE DATABASE inventory_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'inventory_db')\gexec
