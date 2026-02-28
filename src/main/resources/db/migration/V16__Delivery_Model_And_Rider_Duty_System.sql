-- ── 1. Add delivery_model to companies ────────────────────────────
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS delivery_model VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED';

-- ── 2. Add duty + capacity fields to riders ───────────────────────
-- These replace the single isAvailable boolean with a proper model:
--   is_on_duty           → admin sets at shift start/end (once per day)
--   active_order_count   → auto-tracked by system on assign/terminal
--   max_concurrent_orders → 1 for food/instant, 50 for parcel

ALTER TABLE riders
    ADD COLUMN IF NOT EXISTS is_on_duty             BOOLEAN  NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS active_order_count     INTEGER  NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_concurrent_orders  INTEGER  NOT NULL DEFAULT 1;

-- ── 3. Migrate existing rider data ───────────────────────────────
-- Riders that were "available=true" are treated as "on duty, 0 active orders"
-- Riders that were "available=false" were busy — now off duty (safe default)
UPDATE riders
SET is_on_duty           = is_available,
    active_order_count   = CASE WHEN is_available = false THEN 1 ELSE 0 END,
    max_concurrent_orders = 1
WHERE is_on_duty = false
  AND active_order_count = 0;

-- ── 4. Fix any riders stuck with is_available=false from old bug ──
-- These are riders who delivered an order before the free-rider fix was deployed.
-- Only runs if you have existing data. Safe to run on empty DB.
UPDATE riders
SET is_available       = true,
    is_on_duty          = true,
    active_order_count  = 0
WHERE is_available = false
  AND id NOT IN (
    SELECT DISTINCT rider_id
    FROM orders
    WHERE status IN ('ASSIGNED', 'IN_TRANSIT')
      AND rider_id IS NOT NULL
);