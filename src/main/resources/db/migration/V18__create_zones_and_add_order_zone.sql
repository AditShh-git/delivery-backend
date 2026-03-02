--  Create zones table
CREATE TABLE IF NOT EXISTS zones (
                                     id BIGSERIAL PRIMARY KEY,
                                     city VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (city, name)
    );

--  Add zone column to orders (denormalized for fast filtering)
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS zone VARCHAR(100);

--  Backfill existing orders safely (only if null)
UPDATE orders
SET zone = 'UNKNOWN'
WHERE zone IS NULL;

--  Create index for fast zone filtering (assignment, reporting, dispatch)
CREATE INDEX IF NOT EXISTS idx_orders_zone
    ON orders(zone);

-- =============================================
-- Notes:
-- - zone is stored as VARCHAR intentionally (not FK)
-- - Rider.zone already exists and is VARCHAR
-- - SlotCapacity.zone already exists
-- - Zone enforcement happens in application layer
-- =============================================