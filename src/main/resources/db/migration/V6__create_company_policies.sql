CREATE TABLE company_policies (
                                  id                  BIGSERIAL PRIMARY KEY,
                                  company_id          BIGINT      NOT NULL UNIQUE REFERENCES companies(id) ON DELETE CASCADE,
                                  missed_slot_action  VARCHAR(20) NOT NULL DEFAULT 'RESCHEDULE',
                                  max_reschedules     SMALLINT    NOT NULL DEFAULT 2,
                                  penalty_amount      NUMERIC(10, 2),
                                  pickup_checklist    JSONB,
                                  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                  CONSTRAINT chk_missed_slot_action
                                      CHECK (missed_slot_action IN ('RESCHEDULE', 'CHARGE_FEE', 'CANCEL')),

                                  CONSTRAINT chk_max_reschedules
                                      CHECK (max_reschedules >= 0)
);

-- Penalty amount only makes sense when action is CHARGE_FEE
ALTER TABLE company_policies
    ADD CONSTRAINT chk_penalty_requires_charge_fee
        CHECK (
            missed_slot_action != 'CHARGE_FEE'
    OR penalty_amount IS NOT NULL
    );
