-- Week 6: OTP tamper-proof delivery verification
CREATE TABLE delivery_otps (
    id             BIGSERIAL    PRIMARY KEY,
    order_id       BIGINT       NOT NULL,
    otp_hash       VARCHAR(255) NOT NULL,
    expires_at     TIMESTAMP    NOT NULL,
    verified       BOOLEAN      NOT NULL DEFAULT FALSE,
    wrong_attempts INT          NOT NULL DEFAULT 0,
    verified_at    TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT fk_delivery_otp_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- Hot-path index: fetchTopByOrderId sorts by createdAt DESC
CREATE INDEX idx_delivery_otps_order_id ON delivery_otps(order_id);
CREATE INDEX idx_delivery_otps_order_created ON delivery_otps(order_id, created_at DESC);
