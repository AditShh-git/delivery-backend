-- V26: Add GPS coordinates to zones (for RunSheet nearest-neighbor sort)
--      and delivery coordinates to orders (order-level precision, not just zone centroid).
--      All columns are nullable — backward compatible with existing data.
--      Nearest-neighbor sort falls back to insertion order when coords are NULL.

ALTER TABLE zones
    ADD COLUMN lat DOUBLE PRECISION,
    ADD COLUMN lng DOUBLE PRECISION;

ALTER TABLE orders
    ADD COLUMN delivery_lat DOUBLE PRECISION,
    ADD COLUMN delivery_lng DOUBLE PRECISION;
