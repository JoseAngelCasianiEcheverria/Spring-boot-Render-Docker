-- =============================================
-- Esquema PostgreSQL (Supabase / Render)
-- =============================================
-- Equivalente a src/main/resources/schema.sql para el adaptador PostgreSQL.
-- Diferencias con MySQL:
--   ENUM        -> VARCHAR + CHECK
--   ENGINE/CHARSET -> no existen en PostgreSQL
--   ON UPDATE CURRENT_TIMESTAMP -> no existe; el repositorio escribe
--     updated_at = CURRENT_TIMESTAMP en cada UPDATE
-- =============================================

CREATE TABLE IF NOT EXISTS users (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'MEMBER', 'REVIEWER')),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'BLOCKED')),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Trigger opcional. El repositorio ya actualiza updated_at de forma explicita
-- en cada UPDATE, asi que esto es una red de seguridad, no un requisito.
CREATE OR REPLACE FUNCTION touch_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS users_touch_updated_at ON users;

CREATE TRIGGER users_touch_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION touch_updated_at();