package com.jcaa.usersmanagement.infrastructure.adapter.email;

/**
 * Parámetros del adaptador de Brevo por API REST.
 *
 * <p>Es el equivalente de {@link SmtpConfig} para el canal HTTP. Se mantiene separado del de
 * SMTP porque los dos adaptadores nunca están activos a la vez y porque la clave de API de
 * Brevo no sirve para el relay SMTP: son credenciales distintas.
 *
 * @param apiKey clave {@code xkeysib-...} de Brevo. Viaja en la cabecera {@code api-key}.
 * @param fromAddress remitente verificado en Brevo. Nunca el login {@code ...@smtp-brevo.com}.
 * @param fromName nombre legible del remitente.
 * @param baseUrl raíz de la API, sin la ruta final. Configurable para poder apuntar a un stub
 *     en las pruebas sin tocar el código de producción.
 * @param timeoutSeconds tiempo máximo de espera de la llamada HTTP.
 */
public record BrevoConfig(
    String apiKey,
    String fromAddress,
    String fromName,
    String baseUrl,
    int timeoutSeconds) {

  private static final String DEFAULT_BASE_URL = "https://api.brevo.com/v3";
  private static final int DEFAULT_TIMEOUT_SECONDS = 10;
  private static final String PATH_SEND_EMAIL = "/smtp/email";

  /** Ruta completa del endpoint de envío, derivada de {@link #baseUrl}. */
  public String buildSendEmailUrl() {
    final String root =
        (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : trimTrailingSlash(baseUrl);
    return root + PATH_SEND_EMAIL;
  }

  /** Tiempo de espera efectivo, con el valor por defecto cuando no se configuró uno válido. */
  public int effectiveTimeoutSeconds() {
    return timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
  }

  private static String trimTrailingSlash(final String value) {
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}