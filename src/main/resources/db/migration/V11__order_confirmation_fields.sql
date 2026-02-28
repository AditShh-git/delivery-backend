ALTER TABLE orders
    ADD COLUMN customer_confirmed BOOLEAN DEFAULT FALSE,
    ADD COLUMN confirmation_sent_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN reminder_sent BOOLEAN DEFAULT FALSE,
    ADD COLUMN confirmation_attempts INTEGER DEFAULT 0,
    ADD COLUMN auto_cancelled BOOLEAN DEFAULT FALSE;