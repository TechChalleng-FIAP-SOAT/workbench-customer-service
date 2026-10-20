-- =====================================================================
-- Customer Service — schema inicial (Fase 4, Etapa 6)
-- Clientes (CPF/CNPJ) e veículos. Extraído dos módulos client/vehicle do
-- monólito. Vínculo dono-veículo via FK vehicles.client_id → clients.id.
-- =====================================================================

CREATE SEQUENCE client_seq  START 1 INCREMENT 50;
CREATE SEQUENCE vehicle_seq START 1 INCREMENT 50;

CREATE TABLE clients (
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    document    VARCHAR(14)  NOT NULL UNIQUE,   -- CPF (11) ou CNPJ (14), só dígitos
    email       VARCHAR(255),
    phone       VARCHAR(50),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);
CREATE INDEX idx_clients_document ON clients (document);

CREATE TABLE vehicles (
    id          BIGINT PRIMARY KEY,
    client_id   BIGINT NOT NULL REFERENCES clients(id),
    plate       VARCHAR(7) NOT NULL UNIQUE,     -- 7 chars sem hífen (ABC1234 / ABC1D23)
    brand       VARCHAR(255) NOT NULL,
    model       VARCHAR(255) NOT NULL,
    model_year  INTEGER NOT NULL,
    color       VARCHAR(100),
    type        VARCHAR(20) NOT NULL CHECK (type IN ('CAR','MOTORCYCLE','TRUCK','VAN')),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);
CREATE INDEX idx_vehicles_client_id ON vehicles (client_id);
CREATE INDEX idx_vehicles_plate ON vehicles (plate);
