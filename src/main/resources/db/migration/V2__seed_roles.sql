INSERT INTO roles (name) VALUES
                             ('ADMIN'),
                             ('RIDER'),
                             ('CUSTOMER'),
                             ('COMPANY')
    ON CONFLICT (name) DO NOTHING;