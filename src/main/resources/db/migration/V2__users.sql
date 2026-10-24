-- =====================================================================
-- Customer Service como serviço de identidade (paridade com o monólito)
-- Usuários staff (ADMIN/TECHNICIAN) para login via Lambda de auth.
-- =====================================================================

CREATE SEQUENCE user_seq START 1 INCREMENT 50;

CREATE TABLE users (
    id            BIGINT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,          -- BCrypt
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL
);
CREATE INDEX idx_users_email ON users (email);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','TECHNICIAN')),
    PRIMARY KEY (user_id, role)
);

-- Usuário administrador inicial (senha BCrypt de "admin123" — trocar em produção).
INSERT INTO users (id, email, password_hash, active, created_at, updated_at)
VALUES (1, 'admin@workbench.dev',
        '$2a$10$rKWO3t.ssQv0Pxvy7lgu.e1fIGn3efCms9IzQofO9EvEtdL8s3MMu',
        TRUE, NOW(), NOW());

INSERT INTO user_roles (user_id, role) VALUES (1, 'ADMIN');
