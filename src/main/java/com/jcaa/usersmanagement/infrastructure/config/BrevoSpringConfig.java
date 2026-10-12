package com.jcaa.usersmanagement.infrastructure.config;

import com.jcaa.usersmanagement.infrastructure.adapter.email.BrevoConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Carga la configuración del adaptador de Brevo desde las propiedades {@code brevo.*}.
 *
 * <p>Es el equivalente de {@link SmtpSpringConfig} para el canal HTTP.
 */
@Configuration(proxyBeanMethods = false)
public class BrevoSpringConfig {

  private static final String PROP_API_KEY = "${brevo.api-key}";
  private static final String PROP_FROM_ADDRESS = "${brevo.from.address}";
  private static final String PROP_FROM_NAME = "${brevo.from.name}";
  private static final String PROP_BASE_URL = "${brevo.base-url:https://api.brevo.com/v3}";
  private static final String PROP_TIMEOUT_SECONDS = "${brevo.timeout-seconds:10}";

  private static final String KEY_API_KEY = "BREVO_API_KEY";
  private static final String KEY_FROM_ADDRESS = "BREVO_FROM_ADDRESS";

  @Value(PROP_API_KEY)
  private String apiKey;

  @Value(PROP_FROM_ADDRESS)
  private String fromAddress;

  @Value(PROP_FROM_NAME)
  private String fromName;

  @Value(PROP_BASE_URL)
  private String baseUrl;

  @Value(PROP_TIMEOUT_SECONDS)
  private int timeoutSeconds;

  /**
   * Falla de inmediato si falta la clave de API o el remitente.
   *
   * <p>Sin este chequeo la aplicación arranca igual y el problema aparece en cada envío, como
   * un 401 de Brevo que no señala el origen real.
   *
   * @throws ConfigurationException si {@code brevo.api-key} o el remitente vienen vacíos.
   */
  @Bean
  public BrevoConfig brevoConfig() {
    if (isBlank(apiKey)) {
      throw ConfigurationException.becauseMissingValue(KEY_API_KEY);
    }
    if (isBlank(fromAddress)) {
      throw ConfigurationException.becauseMissingValue(KEY_FROM_ADDRESS);
    }
    return new BrevoConfig(apiKey, fromAddress, fromName, baseUrl, timeoutSeconds);
  }

  private static boolean isBlank(final String value) {
    return value == null || value.isBlank();
  }
}