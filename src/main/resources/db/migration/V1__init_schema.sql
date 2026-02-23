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
                           name VARCHAR(150) NOT NULL UNIQUE,
                           contact VARCHAR(150),
                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =========================
-- USERS TABLE
-- =========================
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       full_name VARCHAR(150) NOT NULL,
                       phone VARCHAR(20),
                       company_id BIGINT,
                       enabled BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                       CONSTRAINT fk_user_company
                           FOREIGN KEY (company_id)
                               REFERENCES companies(id)
                               ON DELETE SET NULL
);

-- =========================
-- USER_ROLES (Many-to-Many)
-- =========================
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- =========================
-- RIDERS TABLE
-- =========================
CREATE TABLE riders (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL UNIQUE,
                        vehicle_type VARCHAR(50),
                        license_plate VARCHAR(20),
                        zone VARCHAR(100),
                        is_available BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================
-- ORDERS TABLE
-- =========================
CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        company_id BIGINT NOT NULL,
                        customer_id BIGINT NOT NULL,
                        rider_id BIGINT,
                        status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
                        delivery_address TEXT NOT NULL,
                        items JSONB,
                        attempt_count SMALLINT NOT NULL DEFAULT 0,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                        FOREIGN KEY (company_id) REFERENCES companies(id),
                        FOREIGN KEY (customer_id) REFERENCES users(id),
                        FOREIGN KEY (rider_id) REFERENCES riders(id)
);

-- =========================
-- DEFAULT ROLES
-- =========================
INSERT INTO roles (name) VALUES ('ADMIN');
INSERT INTO roles (name) VALUES ('RIDER');
INSERT INTO roles (name) VALUES ('CUSTOMER');
INSERT INTO roles (name) VALUES ('COMPANY');