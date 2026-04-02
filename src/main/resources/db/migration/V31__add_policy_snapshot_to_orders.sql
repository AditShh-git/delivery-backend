-- V31: Stamp company policy snapshot onto each order at creation time.
-- Denormalized from CompanyPolicy — makes orders audit-safe and reduces
-- repeated policy joins on every status update.
-- All columns nullable: backward compatible with existing orders.

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS policy_max_reschedules     INT,
    ADD COLUMN IF NOT EXISTS policy_missed_slot_action  VARCHAR(50);
