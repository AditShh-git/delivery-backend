-- 1. Add column nullable
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS email VARCHAR(150);

-- 2. Backfill from COMPANY-role user
UPDATE companies c
SET email = u.email
    FROM users u
JOIN user_roles ur ON ur.user_id = u.id
    JOIN roles r ON r.id = ur.role_id
WHERE u.company_id = c.id
  AND r.name = 'COMPANY'
  AND c.email IS NULL;

-- 3. Placeholder for companies without user
UPDATE companies
SET email = CONCAT('company-', id, '@placeholder.internal')
WHERE email IS NULL;

-- 4. Enforce NOT NULL
ALTER TABLE companies
    ALTER COLUMN email SET NOT NULL;

-- 5. Add unique constraint
ALTER TABLE companies
    ADD CONSTRAINT uk_companies_email UNIQUE (email);