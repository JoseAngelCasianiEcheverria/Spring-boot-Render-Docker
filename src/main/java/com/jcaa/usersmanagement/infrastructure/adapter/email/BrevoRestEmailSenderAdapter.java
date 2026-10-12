package com.jcaa.usersmanagement.infrastructure.adapter.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.usersmanagement.application.port.out.EmailSenderPort;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.domain.model.EmailDestinationModel;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adaptador de correo que habla con la API REST de Brevo en vez de con un servidor SMTP.
 *
 * <p>Existe por una razón de infraestructura, no de diseño: el plan gratuito de Render bloquea
 * el SMTP saliente en los puertos 25, 465 y 587. La API REST viaja por HTTPS en el 443, que sí
 * está permitido, así que este adaptador es el único que puede enviar desde Render.
 *
 * <p>Implementa el mismo {@link EmailSenderPort} que {@link JavaMailEmailSenderAdapter}, de modo
 * que el servicio de notificación no cambia. Los dos son {@code @Component} y se separan por
 * perfil, igual que los dos repositorios de persistencia: sin los perfiles excluyentes, Spring
 * encontraría dos candidatos para el mismo puerto y no podría decidir cuál inyectar.
 *
 * <p>Usa el {@link HttpClient} del JDK y Jackson, ya presentes en el proyecto por
 * {@code spring-boot-starter-web}. No agrega ninguna dependencia.
 */
@Slf4j
@Component
@Profile("brevo")
public class BrevoRestEmailSenderAdapter implements EmailSenderPort {

  private static final String HEADER_API_KEY = "api-key";
  private static final String HEADER_CONTENT_TYPE = "Content-Type";
  private static final String HEADER_ACCEPT = "Accept";
  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String METHOD_POST = "POST";
  private static final int HTTP_OK = 201;
  private static final int HTTP_ACCEPTED = 202;
  private static final int MAX_BODY_CHARS = 300;

  private static final String LOG_SENT =
      "[BrevoRestEmailSenderAdapter] correo aceptado por la API. destinatario={}";
  private static final String LOG_REJECTED =
      "[BrevoRestEmailSenderAdapter] la API de Brevo rechazo el envio. status={} detalle={}";

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final BrevoConfig config;

  public BrevoRestEmailSenderAdapter(final BrevoConfig config, final ObjectMapper objectMapper) {
    this.config = config;
    this.objectMapper = objectMapper;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(config.effectiveTimeoutSeconds()))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
  }

  @Override
  public void send(final EmailDestinationModel destination) {
    final String url = config.buildSendEmailUrl();
    final HttpRequest request = buildRequest(url, destination);

    final HttpResponse<String> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (final IOException exception) {
      throw EmailSenderException.becauseTransportFailed(
          destination.getDestinationEmail(), exception.getMessage(), exception);
    } catch (final InterruptedException exception) {
      // Restablecer el flag: sin esto el pool de hilos pierde la cancelacion de forma silenciosa.
      Thread.currentThread().interrupt();
      throw EmailSenderException.becauseTransportFailed(
          destination.getDestinationEmail(), exception.getMessage(), exception);
    }

    if (isAccepted(response.statusCode())) {
      log.info(LOG_SENT, destination.getDestinationEmail());
      return;
    }

    final String detail = truncate(response.body());
    log.warn(LOG_REJECTED, response.statusCode(), detail);
    throw EmailSenderException.becauseApiFailed(
        destination.getDestinationEmail(), response.statusCode(), detail);
  }

  private HttpRequest buildRequest(final String url, final EmailDestinationModel destination) {
    final BrevoEmailPayload payload =
        new BrevoEmailPayload(
            config.fromAddress(),
            config.fromName(),
            destination.getDestinationEmail(),
            destination.getDestinationName(),
            destination.getSubject(),
            destination.getBody());

    final String json;
    try {
      json = objectMapper.writeValueAsString(payload);
    } catch (final JsonProcessingException exception) {
      throw EmailSenderException.becauseSendFailed(exception);
    }

    return HttpRequest.newBuilder(URI.create(url))
        .header(HEADER_API_KEY, config.apiKey())
        .header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
        .header(HEADER_ACCEPT, CONTENT_TYPE_JSON)
        .timeout(Duration.ofSeconds(config.effectiveTimeoutSeconds()))
        .method(METHOD_POST, HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
        .build();
  }

  private static boolean isAccepted(final int statusCode) {
    return statusCode == HTTP_ACCEPTED || statusCode == HTTP_OK;
  }

  private static String truncate(final String body) {
    if (body == null) {
      return "";
    }
    return body.length() <= MAX_BODY_CHARS ? body : body.substring(0, MAX_BODY_CHARS);
  }
}