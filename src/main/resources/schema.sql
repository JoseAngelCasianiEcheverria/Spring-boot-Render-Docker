-- =============================================
-- Script de creación de la base de datos
-- Gestión de Usuarios - Arquitectura Hexagonal
-- =============================================

CREATE DATABASE IF NOT EXISTS crud_usuarios
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE crud_usuarios;

CREATE TABLE IF NOT EXISTS users (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('ADMIN', 'MEMBER', 'REVIEWER') NOT NULL,
    status      ENUM('ACTIVE', 'INACTIVE', 'PENDING', 'BLOCKED') NOT NULL DEFAULT 'PENDING',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- NOTA: este script solo crea el esquema. NO crea ningun usuario inicial.
-- Sembrar el administrador es una operacion manual y deliberada, porque una
-- credencial de administrador versionada en el repositorio equivale a una
-- credencial publica. Ver README.md, seccion "Primer arranque".
--
-- Para crear tu propio administrador, genera el hash y ejecuta el INSERT:
--   [Convert]::ToBase64String(...)  /  ver README
--   mysql -uusers_app -p crud_usuarios < db/local-seed.sql

