-- V27: RunSheet (PARCEL) — run sheet + run sheet orders tables

CREATE TABLE run_sheets (
    id          BIGSERIAL    PRIMARY KEY,
    rider_id    BIGINT       NOT NULL REFERENCES riders(id),
    zone        VARCHAR(50)  NOT NULL,
    slot_date   DATE         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',  -- DRAFT | LOCKED
    created_by  BIGINT       NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (rider_id, slot_date)
);

CREATE TABLE run_sheet_orders (
    id              BIGSERIAL  PRIMARY KEY,
    run_sheet_id    BIGINT     NOT NULL REFERENCES run_sheets(id),
    order_id        BIGINT     NOT NULL REFERENCES orders(id),
    sequence_num    INTEGER    NOT NULL DEFAULT 0,
    UNIQUE (run_sheet_id, order_id)
);

-- Index: every /sort and /export query filters by run_sheet_id
CREATE INDEX idx_rso_run_sheet_id ON run_sheet_orders(run_sheet_id);
