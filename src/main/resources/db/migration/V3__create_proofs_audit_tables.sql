CREATE TABLE proofs (
                        id                  BIGSERIAL PRIMARY KEY,
                        order_id            BIGINT NOT NULL REFERENCES orders(id),
                        rider_id            BIGINT NOT NULL REFERENCES riders(id),
                        attempt_number      SMALLINT NOT NULL DEFAULT 1,
                        gps_lat             DECIMAL(9,6),
                        gps_lng             DECIMAL(9,6),
                        photo_url           VARCHAR(500),
                        call_log_ref        VARCHAR(255),
                        wait_duration_secs  INTEGER,
                        submitted_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Immutable — no updated_at by design
                        CONSTRAINT proofs_non_deletable CHECK (true)
);

CREATE INDEX idx_proofs_order_id ON proofs(order_id);
CREATE INDEX idx_proofs_rider_id ON proofs(rider_id);

CREATE TABLE audit_log (
                           id          BIGSERIAL PRIMARY KEY,
                           entity_type VARCHAR(50) NOT NULL,   -- 'ORDER', 'USER', 'RIDER'
                           entity_id   BIGINT NOT NULL,
                           action      VARCHAR(50) NOT NULL,   -- 'STATUS_CHANGE', 'ASSIGNED', 'CREATED'
                           performed_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
                           old_value   TEXT,
                           new_value   TEXT,
                           created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_performed_by ON audit_log(performed_by);