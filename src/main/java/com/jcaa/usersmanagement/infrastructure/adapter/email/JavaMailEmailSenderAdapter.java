package com.jcaa.usersmanagement.infrastructure.adapter.email;

import com.jcaa.usersmanagement.application.port.out.EmailSenderPort;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.domain.model.EmailDestinationModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Adaptador de correo por SMTP con JavaMail, el de por defecto.
 *
 * <p>Implementa el mismo {@link EmailSenderPort} que {@link BrevoRestEmailSenderAdapter}, por
 * eso ambos compiten por el mismo tipo de bean y por eso lleva {@code @Profile("!brevo")}: sin
 * él, al activar el perfil {@code brevo}, Spring encontraría dos candidatos para el puerto y no
 * podría decidir cuál inyectar. Es el mismo motivo por el que
 * {@link com.jcaa.usersmanagement.infrastructure.adapter.persistence.repository.UserRepositoryMySQL}
 * lleva {@code !postgres}.
 *
 * <p>Nota de infraestructura: desde el plan gratuito de Render este adaptador no puede enviar,
 * porque la plataforma bloquea el SMTP saliente en 25, 465 y 587. Sigue siendo el adaptador
 * correcto para desarrollo local.
 */
@Slf4j
@Component
@Profile("!brevo")
public class JavaMailEmailSenderAdapter implements EmailSenderPort {

  private static final String MAIL_SMTP_HOST = "mail.smtp.host";
  private static final String MAIL_SMTP_PORT = "mail.smtp.port";
  private static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
  private static final String MAIL_SMTP_STARTTLS = "mail.smtp.starttls.enable";
  private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";
  private static final String CHARSET_UTF8 = "UTF-8";
  private static final String LOG_SENT = "[JavaMailEmailSenderAdapter] correo enviado exitosamente.";

  private final Session mailSession;
  private final String fromAddress;
  private final String fromName;

  public JavaMailEmailSenderAdapter(final SmtpConfig config) {
    this.fromAddress = config.fromAddress();
    this.fromName = config.fromName();
    this.mailSession = buildSession(config);
  }

  @Override
  public void send(final EmailDestinationModel destination) {
    try {
      final MimeMessage message = buildMessage(destination);
      Transport.send(message);
      log.info(LOG_SENT);
    } catch (final MessagingException | UnsupportedEncodingException exception) {
      throw EmailSenderException.becauseSmtpFailed(
          destination.getDestinationEmail(), exception.getMessage());
    }
  }

  private MimeMessage buildMessage(final EmailDestinationModel destination)
      throws MessagingException, UnsupportedEncodingException {
    final MimeMessage message = new MimeMessage(mailSession);
    message.setFrom(new InternetAddress(fromAddress, fromName, CHARSET_UTF8));
    message.addRecipient(
        Message.RecipientType.TO,
        new InternetAddress(
            destination.getDestinationEmail(), destination.getDestinationName(), CHARSET_UTF8));
    message.setSubject(destination.getSubject(), CHARSET_UTF8);
    message.setContent(destination.getBody(), CONTENT_TYPE_HTML);
    return message;
  }

  private static Session buildSession(final SmtpConfig config) {
    final Properties properties = buildSmtpProperties(config);
    return Session.getInstance(
        properties,
        new Authenticator() {
          @Override
          protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(config.username(), config.password());
          }
        });
  }

  private static Properties buildSmtpProperties(final SmtpConfig config) {
    final Properties properties = new Properties();
    properties.put(MAIL_SMTP_HOST, config.host());
    properties.put(MAIL_SMTP_PORT, String.valueOf(config.port()));
    properties.put(MAIL_SMTP_AUTH, "true");
    properties.put(MAIL_SMTP_STARTTLS, "true");
    return properties;
  }
}
