CREATE TABLE slot_capacities (
                                 id BIGSERIAL PRIMARY KEY,

                                 company_id BIGINT NOT NULL,
                                 zone VARCHAR(100) NOT NULL,
                                 slot_date DATE NOT NULL,
                                 slot_label VARCHAR(50) NOT NULL,

                                 capacity INTEGER NOT NULL,
                                 booked_count INTEGER NOT NULL DEFAULT 0,

                                 created_at TIMESTAMP WITH TIME ZONE,
                                 updated_at TIMESTAMP WITH TIME ZONE,

                                 CONSTRAINT fk_slot_company
                                     FOREIGN KEY (company_id)
                                         REFERENCES companies(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT uk_slot_capacity
                                     UNIQUE (company_id, zone, slot_date, slot_label)
);