ALTER TABLE orders
    ADD COLUMN missed_slot_count INTEGER DEFAULT 0,
    ADD COLUMN penalty_applied BOOLEAN DEFAULT FALSE;