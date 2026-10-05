package com.jcaa.usersmanagement.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private static final String FORBIDDEN_MESSAGE = "No tiene permisos para realizar esta operacion.";

  private final ObjectMapper objectMapper;

  public RestAccessDeniedHandler(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final AccessDeniedException exception)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(),
        new ApiErrorResponse(HttpServletResponse.SC_FORBIDDEN, FORBIDDEN_MESSAGE));
  }
}
