-- ── New columns ──────────────────────────────────────────────

ALTER TABLE orders
    ADD COLUMN order_type          VARCHAR(20),
    ADD COLUMN delivery_type       VARCHAR(20),
    ADD COLUMN slot_label          VARCHAR(50),
    ADD COLUMN slot_date           DATE,
    ADD COLUMN external_order_id   VARCHAR(100),
    ADD COLUMN product_category    VARCHAR(100),
    ADD COLUMN call_before_arrival BOOLEAN NOT NULL DEFAULT FALSE;

-- ── Backfill existing rows so NOT NULL can be added ──────────
-- (only relevant if you have existing data — safe to run on empty table too)

UPDATE orders SET order_type = 'DELIVERY' WHERE order_type IS NULL;

-- ── Enforce NOT NULL on order_type going forward ─────────────

ALTER TABLE orders
    ALTER COLUMN order_type SET NOT NULL;

-- ── Unique constraint: one external_order_id per company ─────
-- Critical for ecom push deduplication

CREATE UNIQUE INDEX uq_company_external_order
    ON orders(company_id, external_order_id)
    WHERE external_order_id IS NOT NULL;   -- partial index: skips rows where ecom ID not provided

-- ── Performance indexes ──────────────────────────────────────

CREATE INDEX idx_orders_status     ON orders(status);
CREATE INDEX idx_orders_company_id ON orders(company_id);
CREATE INDEX idx_orders_rider_id   ON orders(rider_id);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_orders_slot_date  ON orders(slot_date);
