package com.jcaa.usersmanagement.domain.exception;

public final class EmailSenderException extends DomainException {

  private static final String DEFAULT_MESSAGE = "La notificación por correo no pudo ser enviada.";
  private static final String MESSAGE_WITH_DETAIL =
      "No se pudo enviar el correo a '%s'. Error SMTP: %s";
  private static final String MESSAGE_WITH_API_DETAIL =
      "No se pudo enviar el correo a '%s'. La API de Brevo respondió %d: %s";
  private static final String MESSAGE_WITH_TRANSPORT_DETAIL =
      "No se pudo enviar el correo a '%s'. Fallo de transporte: %s";

  public EmailSenderException(final String message) {
    super(message);
  }

  public EmailSenderException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static EmailSenderException becauseSmtpFailed(
      final String destinationEmail, final String smtpError) {
    return new EmailSenderException(
        String.format(MESSAGE_WITH_DETAIL, destinationEmail, smtpError));
  }

  public static EmailSenderException becauseSendFailed(final Throwable cause) {
    return new EmailSenderException(DEFAULT_MESSAGE, cause);
  }

  /**
   * La API de Brevo respondio, pero con un codigo de error.
   *
   * <p>Se distingue de {@link #becauseSmtpFailed} porque el puerto no debe conocer el transporte:
   * el servicio de notificacion registra el fallo y sigue, y el mensaje necesita decir de donde
   * vino para que el operador sepa si es la clave, el remitente o el servicio.
   *
   * @param destinationEmail destinatario que se quiso notificar.
   * @param statusCode codigo HTTP devuelto por la API.
   * @param detail cuerpo de la respuesta, ya recortado por el adaptador.
   */
  public static EmailSenderException becauseApiFailed(
      final String destinationEmail, final int statusCode, final String detail) {
    return new EmailSenderException(
        String.format(MESSAGE_WITH_API_DETAIL, destinationEmail, statusCode, detail));
  }

  /** La llamada ni siquiera llego a la API: fallo de red, DNS o tiempo de espera. */
  public static EmailSenderException becauseTransportFailed(
      final String destinationEmail, final String transportError, final Throwable cause) {
    return new EmailSenderException(
        String.format(MESSAGE_WITH_TRANSPORT_DETAIL, destinationEmail, transportError), cause);
  }
}
