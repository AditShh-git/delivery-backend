-- V25: Drop requires_otp from company_policies.
--
-- OTP verification is now a platform-level invariant enforced unconditionally
-- in OrderServiceImpl.ensureOtpVerified() for every DELIVERED and COLLECTED
-- transition. The requires_otp column was added in V23 as a per-company toggle,
-- but that design was superseded: no code reads this column and the flag has no
-- effect. Keeping it in the schema is misleading — it implies the check is
-- opt-in when it is not. Dropping it removes the ambiguity.

ALTER TABLE company_policies
    DROP COLUMN IF EXISTS requires_otp;
