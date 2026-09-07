-- ─────────────────────────────────────────────────────────────────────────────
-- Schema de produção para o MCC Credit Assignment API (PostgreSQL / AWS RDS).
-- Reflete as entidades JPA em src/main/java/.../infrastructure/adapter/out/persistence/entity.
-- Script idempotente: pode ser reexecutado com segurança (IF NOT EXISTS).
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS assignors (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    document    VARCHAR(14) NOT NULL UNIQUE,
    email       VARCHAR(150) NOT NULL,
    active      BOOLEAN NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS receivables (
    id                VARCHAR(36) PRIMARY KEY,
    assignor_id       VARCHAR(36) NOT NULL REFERENCES assignors(id),
    type              VARCHAR(30) NOT NULL CHECK (type IN ('DUPLICATA', 'POST_DATED_CHECK')),
    face_value        NUMERIC(19,4) NOT NULL,
    asset_currency    VARCHAR(3) NOT NULL CHECK (asset_currency IN ('BRL', 'USD', 'EUR')),
    payment_currency  VARCHAR(3) NOT NULL CHECK (payment_currency IN ('BRL', 'USD', 'EUR')),
    maturity_date     DATE NOT NULL,
    term_months       INTEGER NOT NULL,
    status            VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SETTLED', 'CANCELLED')),
    created_at        TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_receivables_assignor ON receivables(assignor_id);
CREATE INDEX IF NOT EXISTS idx_receivables_status ON receivables(status);

CREATE TABLE IF NOT EXISTS settlements (
    id                        VARCHAR(36) PRIMARY KEY,
    receivable_id             VARCHAR(36) NOT NULL REFERENCES receivables(id),
    base_rate                 NUMERIC(19,10) NOT NULL,
    present_value             NUMERIC(19,10) NOT NULL,
    exchange_rate_used        NUMERIC(19,10),
    present_value_converted   NUMERIC(19,10) NOT NULL,
    payment_currency          VARCHAR(3) NOT NULL CHECK (payment_currency IN ('BRL', 'USD', 'EUR')),
    settled_at                TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_settlements_receivable ON settlements(receivable_id);
CREATE INDEX IF NOT EXISTS idx_settlements_settled_at ON settlements(settled_at);
CREATE INDEX IF NOT EXISTS idx_settlements_payment_currency ON settlements(payment_currency);

CREATE TABLE IF NOT EXISTS users (
    id          VARCHAR(36) PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    role        VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'OPERATOR', 'VIEWER')),
    enabled     BOOLEAN NOT NULL,
    created_at  TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS exchange_rates (
    id             VARCHAR(36) PRIMARY KEY,
    from_currency  VARCHAR(3) NOT NULL CHECK (from_currency IN ('BRL', 'USD', 'EUR')),
    to_currency    VARCHAR(3) NOT NULL CHECK (to_currency IN ('BRL', 'USD', 'EUR')),
    rate           NUMERIC(19,10) NOT NULL,
    source         VARCHAR(20) NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    CONSTRAINT uk_exchange_rates_pair UNIQUE (from_currency, to_currency)
);
