-- V21: Add GPS coordinates and photo_url to attempt_history for audit-grade failure proof.
-- All columns are nullable for backward compatibility with existing rows.

ALTER TABLE attempt_history
    ADD COLUMN latitude  DECIMAL(9, 6),
    ADD COLUMN longitude DECIMAL(9, 6),
    ADD COLUMN photo_url VARCHAR(500);
