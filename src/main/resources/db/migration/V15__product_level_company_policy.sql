-- 1️⃣ Drop old unique constraint (if exists)

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'company_policies_company_id_key'
    )
    THEN
ALTER TABLE company_policies
DROP CONSTRAINT company_policies_company_id_key;
END IF;
END $$;


-- 2️⃣ Add new columns

ALTER TABLE company_policies
    ADD COLUMN product_category VARCHAR(100),
    ADD COLUMN delivery_type VARCHAR(50);


-- 3️⃣ Set default values for existing rows
-- (So migration doesn't fail due to NOT NULL later)

UPDATE company_policies
SET product_category = 'DEFAULT_PRODUCT',
    delivery_type = 'STANDARD'
WHERE product_category IS NULL;


-- 4️⃣ Make new columns NOT NULL

ALTER TABLE company_policies
    ALTER COLUMN product_category SET NOT NULL,
    ALTER COLUMN delivery_type SET NOT NULL;


-- 5️⃣ Add new composite unique constraint

ALTER TABLE company_policies
    ADD CONSTRAINT uk_company_product_delivery
        UNIQUE (company_id, product_category, delivery_type);