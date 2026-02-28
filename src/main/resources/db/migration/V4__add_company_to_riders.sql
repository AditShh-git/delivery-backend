ALTER TABLE riders
    ADD COLUMN company_id BIGINT;

ALTER TABLE riders
    ADD CONSTRAINT fk_rider_company
        FOREIGN KEY (company_id)
            REFERENCES companies(id);

CREATE INDEX idx_riders_company_id ON riders(company_id);


ALTER TABLE riders
    ALTER COLUMN company_id SET NOT NULL;