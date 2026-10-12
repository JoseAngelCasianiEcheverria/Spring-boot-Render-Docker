package com.jcaa.usersmanagement.infrastructure.adapter.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.domain.model.EmailDestinationModel;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for BrevoRestEmailSenderAdapter.
 *
 * <p>Levanta un servidor HTTP real en un puerto efimero en vez de simular el cliente. Motivo:
 * el adaptador construye la URL, las cabeceras y el cuerpo, y un mock de HttpClient verificaria
 * solo que se llamo al mock, no que la peticion que sale a internet sea correcta. El stub
 * captura lo que llego de verdad.
 *
 * <p>Ninguna prueba toca api.brevo.com.
 */
@DisplayName("BrevoRestEmailSenderAdapter")
class BrevoRestEmailSenderAdapterTest {

  private static final String API_KEY = "xkeysib-test-key";
  private static final String FROM_ADDRESS = "remitente@example.com";
  private static final String FROM_NAME = "Gestion de Usuarios";
  private static final String DEST_EMAIL = "john@example.com";
  private static final String DEST_NAME = "John Doe";
  private static final String SUBJECT = "Cuenta creada";
  private static final String BODY = "<html><body>Bienvenido</body></html>";

  private HttpServer server;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AtomicReference<String> receivedApiKey = new AtomicReference<>();
  private final AtomicReference<String> receivedBody = new AtomicReference<>();
  private final AtomicReference<String> receivedPath = new AtomicReference<>();
  private final AtomicReference<String> receivedMethod = new AtomicReference<>();

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/v3/smtp/email", exchange -> {
      receivedApiKey.set(exchange.getRequestHeaders().getFirst("api-key"));
      receivedPath.set(exchange.getRequestURI().getPath());
      receivedMethod.set(exchange.getRequestMethod());
      receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      respond(exchange, 201, "{\"messageId\":\"<abc@brevo>\"}");
    });
    server.start();
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  private void respond(final com.sun.net.httpserver.HttpExchange exchange, final int status,
      final String body) throws IOException {
    final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  private BrevoRestEmailSenderAdapter adapterFor(final String path) {
    final BrevoConfig config =
        new BrevoConfig(
            API_KEY, FROM_ADDRESS, FROM_NAME, "http://127.0.0.1:" + port() + path, 5);
    return new BrevoRestEmailSenderAdapter(config, objectMapper);
  }

  private int port() {
    return server.getAddress().getPort();
  }

  private EmailDestinationModel destination() {
    return new EmailDestinationModel(DEST_EMAIL, DEST_NAME, SUBJECT, BODY);
  }

  // ── send() — camino feliz

  @Test
  @DisplayName("send() returns without throwing when Brevo answers 201")
  void shouldNotThrowWhenApiAccepts() {
    adapterFor("/v3").send(destination());
    assertThat(receivedMethod.get()).isEqualTo("POST");
    assertThat(receivedPath.get()).isEqualTo("/v3/smtp/email");
  }

  @Test
  @DisplayName("send() sends the api-key in the header and the payload as JSON in the body")
  void shouldSendApiKeyHeaderAndJsonBody() throws Exception {
    adapterFor("/v3").send(destination());

    assertThat(receivedApiKey.get()).isEqualTo(API_KEY);

    final JsonNode json = objectMapper.readTree(receivedBody.get());
    assertThat(json.at("/sender/email").asText()).isEqualTo(FROM_ADDRESS);
    assertThat(json.at("/sender/name").asText()).isEqualTo(FROM_NAME);
    assertThat(json.at("/to/0/email").asText()).isEqualTo(DEST_EMAIL);
    assertThat(json.at("/to/0/name").asText()).isEqualTo(DEST_NAME);
    assertThat(json.at("/subject").asText()).isEqualTo(SUBJECT);
    assertThat(json.at("/htmlContent").asText()).isEqualTo(BODY);
  }

  @Test
  @DisplayName("buildSendEmailUrl() accepts a base URL with or without a trailing slash")
  void shouldNormalizeTrailingSlashInBaseUrl() {
    final BrevoConfig conSlash = new BrevoConfig(API_KEY, FROM_ADDRESS, FROM_NAME, "https://x/v3/", 5);
    final BrevoConfig sinSlash = new BrevoConfig(API_KEY, FROM_ADDRESS, FROM_NAME, "https://x/v3", 5);
    assertThat(conSlash.buildSendEmailUrl()).isEqualTo("https://x/v3/smtp/email");
    assertThat(sinSlash.buildSendEmailUrl()).isEqualTo("https://x/v3/smtp/email");
  }

  // ── send() — 202 tambien es aceptacion

  @Test
  @DisplayName("send() accepts 202 as well as 201")
  void shouldTreatAcceptedAsSuccess() {
    server.removeContext("/v3/smtp/email");
    server.createContext("/v3/smtp/email", exchange -> {
      respond(exchange, 202, "{}");
    });
    adapterFor("/v3").send(destination());
  }

  // ── send() — error de la API

  @Test
  @DisplayName("send() throws EmailSenderException with status and body when Brevo rejects")
  void shouldThrowWithStatusAndBodyWhenApiRejects() {
    server.removeContext("/v3/smtp/email");
    server.createContext("/v3/smtp/email", exchange -> {
      respond(exchange, 401, "{\"code\":\"unauthorized\",\"message\":\"Key not found\"}");
    });

    final EmailSenderException exception =
        assertThrows(EmailSenderException.class, () -> adapterFor("/v3").send(destination()));

    assertThat(exception.getMessage()).contains(DEST_EMAIL);
    assertThat(exception.getMessage()).contains("401");
    assertThat(exception.getMessage()).contains("Key not found");
  }

  @Test
  @DisplayName("send() throws when the response body is very long and must be truncated")
  void shouldTruncateHugeResponseBody() {
    server.removeContext("/v3/smtp/email");
    final List<String> grandes = new ArrayList<>();
    for (int i = 0; i < 200; i++) {
      grandes.add("detalle muy largo que excede el limite");
    }
    final String enorme = String.join(" ", grandes);
    server.createContext("/v3/smtp/email", exchange -> respond(exchange, 400, enorme));

    final EmailSenderException exception =
        assertThrows(EmailSenderException.class, () -> adapterFor("/v3").send(destination()));

    assertThat(exception.getMessage()).hasSizeLessThan(400);
  }

  // ── send() — fallo de transporte

  @Test
  @DisplayName("send() throws when the endpoint cannot be reached at all")
  void shouldThrowWhenTransportFails() {
    final BrevoConfig config =
        new BrevoConfig(API_KEY, FROM_ADDRESS, FROM_NAME, "http://127.0.0.1:1/v3", 2);
    final BrevoRestEmailSenderAdapter adapter =
        new BrevoRestEmailSenderAdapter(config, objectMapper);

    final EmailSenderException exception =
        assertThrows(EmailSenderException.class, () -> adapter.send(destination()));

    assertThat(exception.getMessage()).contains(DEST_EMAIL);
  }
}