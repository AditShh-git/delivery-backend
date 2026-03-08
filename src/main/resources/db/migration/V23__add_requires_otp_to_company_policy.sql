-- Week 6: opt-in OTP enforcement per company policy row
ALTER TABLE company_policies
    ADD COLUMN requires_otp BOOLEAN NOT NULL DEFAULT FALSE;
