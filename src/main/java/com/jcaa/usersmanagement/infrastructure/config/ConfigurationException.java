package com.jcaa.usersmanagement.infrastructure.config;

public final class ConfigurationException extends RuntimeException {

  private static final String MESSAGE_LOAD = "Failed to load the application configuration.";
  private static final String MESSAGE_MISSING_VALUE = "Falta el valor de '%s' en la configuración.";

  private ConfigurationException(final String message, final Throwable cause) {
    super(message, cause);
  }

  private ConfigurationException(final String message) {
    super(message);
  }

  public static ConfigurationException becauseLoadFailed(final Throwable cause) {
    return new ConfigurationException(MESSAGE_LOAD, cause);
  }

  public static ConfigurationException becauseMissingValue(final String key) {
    return new ConfigurationException(String.format(MESSAGE_MISSING_VALUE, key));
  }
}
