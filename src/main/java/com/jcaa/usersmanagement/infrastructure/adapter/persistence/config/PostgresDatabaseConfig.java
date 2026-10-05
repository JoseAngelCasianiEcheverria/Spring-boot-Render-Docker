package com.jcaa.usersmanagement.infrastructure.adapter.persistence.config;

/**
 * Configuración de conexión para PostgreSQL (Supabase / Render).
 *
 * <p>Diferencias con {@link DatabaseConfig} que importan:
 *
 * <ul>
 *   <li>El driver es {@code jdbc:postgresql://} en lugar de {@code jdbc:mysql://}.
 *   <li>El parámetro de cifrado se llama {@code sslmode} (todo minúsculas) y su vocabulario es
 *       {@code disable|allow|prefer|require|verify-ca|verify-full}. El de MySQL es
 *       {@code DISABLED|PREFERRED|REQUIRED...}. Por eso NO se reutiliza {@code db.ssl-mode}: se
 *       usa {@code db.postgres.sslmode}, en minúsculas.
 *   <li>No se pasan {@code serverTimezone} ni {@code allowPublicKeyRetrieval}: son específicos del
 *       driver de MySQL y PostgreSQL los rechazaría.
 * </ul>
 *
 * <p>El default es {@code require} porque Supabase lo exige. Contra un PostgreSQL local sin TLS hay
 * que cambiarlo a {@code disable} (ver README).
 */
public record PostgresDatabaseConfig(
    String host, int port, String databaseName, String username, String password, String sslMode) {

  private static final String URL_TEMPLATE = "jdbc:postgresql://%s:%d/%s?sslmode=%s";

  public String buildJdbcUrl() {
    return String.format(URL_TEMPLATE, host, port, databaseName, sslMode);
  }
}