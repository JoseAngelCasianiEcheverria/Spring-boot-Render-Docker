package com.jcaa.usersmanagement.infrastructure.adapter.email;

/**
 * Cuerpo de la petición a {@code POST /v3/smtp/email} de Brevo.
 *
 * <p>Es un tipo separado del adaptador y no usa mapas sueltos a propósito: si Brevo cambia el
 * contrato, el compilador señala todos los sitios afectados en lugar de dejar un
 * {@code Map<String, Object>} que revienta en tiempo de ejecución.
 *
 * @param sender remitente verificado.
 * @param to lista de destinatarios. Brevo acepta varias direcciones en la misma llamada.
 * @param subject asunto del mensaje.
 * @param htmlContent cuerpo en HTML. El dominio ya entrega el cuerpo en HTML.
 */
public record BrevoEmailPayload(Sender sender, java.util.List<Recipient> to, String subject,
    String htmlContent) {

  public BrevoEmailPayload(
      final String fromAddress, final String fromName, final String toAddress,
      final String toName, final String subject, final String htmlContent) {
    this(new Sender(fromName, fromAddress), java.util.List.of(new Recipient(toAddress, toName)),
        subject, htmlContent);
  }

  /**
   * @param name nombre legible del remitente.
   * @param email dirección del remitente. Debe coincidir con un remitente verificado.
   */
  public record Sender(String name, String email) {}

  /**
   * @param email dirección del destinatario.
   * @param name nombre legible del destinatario.
   */
  public record Recipient(String email, String name) {}
}