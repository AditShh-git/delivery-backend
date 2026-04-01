-- V24: Drop the proofs table introduced in V3.
--
-- The proofs table was created in V3 as a planned photo/signature audit store.
-- It was superseded by the attempt_history table (V9), which was extended in V21
-- to store GPS coordinates (latitude, longitude) and photo_url directly on each
-- failed delivery attempt. Nothing in the application writes to proofs — it is
-- dead schema weight that misleads anyone reading the DB.

DROP TABLE IF EXISTS proofs;
