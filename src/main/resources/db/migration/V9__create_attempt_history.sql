CREATE TABLE attempt_history (
                                 id BIGSERIAL PRIMARY KEY,
                                 order_id BIGINT NOT NULL,
                                 rider_id BIGINT,
                                 attempt_number INTEGER NOT NULL,
                                 failure_reason VARCHAR(255),
                                 recorded_by VARCHAR(50) NOT NULL,
                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT fk_attempt_order
                                     FOREIGN KEY (order_id) REFERENCES orders(id),

                                 CONSTRAINT fk_attempt_rider
                                     FOREIGN KEY (rider_id) REFERENCES riders(id)
);