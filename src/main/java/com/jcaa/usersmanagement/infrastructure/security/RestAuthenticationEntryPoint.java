package com.jcaa.usersmanagement.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private static final String UNAUTHORIZED_MESSAGE = "Autenticacion requerida o token invalido.";

  private final ObjectMapper objectMapper;

  public RestAuthenticationEntryPoint(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final AuthenticationException exception)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(),
        new ApiErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED_MESSAGE));
  }
}
