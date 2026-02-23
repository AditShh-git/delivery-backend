-- =========================
-- ROLES TABLE
-- =========================
CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE
);

-- =========================
-- COMPANIES TABLE
-- =========================
CREATE TABLE companies (
                           id BIGSERIAL PRIMARY KEY,
                           name VARCHAR(150) NOT NULL,
                           contact_email VARCHAR(150),
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- USERS TABLE
-- =========================
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(150) NOT NULL,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role_id BIGINT NOT NULL,
                       company_id BIGINT,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT fk_user_role
                           FOREIGN KEY (role_id)
                               REFERENCES roles(id)
                               ON DELETE RESTRICT,

                       CONSTRAINT fk_user_company
                           FOREIGN KEY (company_id)
                               REFERENCES companies(id)
                               ON DELETE SET NULL
);

-- =========================
-- RIDERS TABLE
-- =========================
CREATE TABLE riders (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL UNIQUE,
                        active BOOLEAN DEFAULT TRUE,

                        CONSTRAINT fk_rider_user
                            FOREIGN KEY (user_id)
                                REFERENCES users(id)
                                ON DELETE CASCADE
);

-- =========================
-- ORDERS TABLE
-- =========================
CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        customer_id BIGINT NOT NULL,
                        company_id BIGINT NOT NULL,
                        rider_id BIGINT,
                        status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
                        attempt_count INT DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_order_customer
                            FOREIGN KEY (customer_id)
                                REFERENCES users(id)
                                ON DELETE RESTRICT,

                        CONSTRAINT fk_order_company
                            FOREIGN KEY (company_id)
                                REFERENCES companies(id)
                                ON DELETE RESTRICT,

                        CONSTRAINT fk_order_rider
                            FOREIGN KEY (rider_id)
                                REFERENCES riders(id)
                                ON DELETE SET NULL
);

-- =========================
-- AUDIT LOG TABLE
-- =========================
CREATE TABLE audit_log (
                           id BIGSERIAL PRIMARY KEY,
                           action VARCHAR(150) NOT NULL,
                           performed_by BIGINT,
                           performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_audit_user
                               FOREIGN KEY (performed_by)
                                   REFERENCES users(id)
                                   ON DELETE SET NULL
);

-- =========================
-- DEFAULT ROLE DATA
-- =========================
INSERT INTO roles (name) VALUES ('ADMIN');
INSERT INTO roles (name) VALUES ('RIDER');
INSERT INTO roles (name) VALUES ('CUSTOMER');
INSERT INTO roles (name) VALUES ('COMPANY');