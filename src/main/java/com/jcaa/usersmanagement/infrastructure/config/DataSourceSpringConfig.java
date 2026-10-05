package com.jcaa.usersmanagement.infrastructure.config;

import com.jcaa.usersmanagement.infrastructure.adapter.persistence.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class DataSourceSpringConfig {

  private static final String PROP_DB_HOST     = "${db.host}";
  private static final String PROP_DB_PORT     = "${db.port}";
  private static final String PROP_DB_NAME     = "${db.name}";
  private static final String PROP_DB_USERNAME = "${db.username}";
  private static final String PROP_DB_PASSWORD = "${db.password}";
  private static final String PROP_DB_SSL_MODE = "${db.ssl-mode}";

  private static final String LOG_DATASOURCE_INIT = "[DataSourceSpringConfig] DataSource inicializado. host={} port={}";

  @Value(PROP_DB_HOST)
  private String dbHost;

  @Value(PROP_DB_PORT)
  private int dbPort;

  @Value(PROP_DB_NAME)
  private String dbName;

  @Value(PROP_DB_USERNAME)
  private String dbUsername;

  @Value(PROP_DB_PASSWORD)
  private String dbPassword;

  @Value(PROP_DB_SSL_MODE)
  private String dbSslMode;

  @Bean
  public DataSource dataSource() {
    final DatabaseConfig config =
        new DatabaseConfig(dbHost, dbPort, dbName, dbUsername, dbPassword, dbSslMode);

    final HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(config.buildJdbcUrl());
    hikariConfig.setUsername(config.username());
    hikariConfig.setPassword(config.password());
    hikariConfig.setMaximumPoolSize(10);
    hikariConfig.setMinimumIdle(2);
    hikariConfig.setConnectionTimeout(30_000);

    log.info(LOG_DATASOURCE_INIT, dbHost, dbPort);
    return new HikariDataSource(hikariConfig);
  }
}

