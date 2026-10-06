package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jcaa.usersmanagement.application.port.in.CreateUserUseCase;
import com.jcaa.usersmanagement.application.port.in.DeleteUserUseCase;
import com.jcaa.usersmanagement.application.port.in.GetAllUsersUseCase;
import com.jcaa.usersmanagement.application.port.in.GetUserByIdUseCase;
import com.jcaa.usersmanagement.application.port.in.UpdateUserUseCase;
import com.jcaa.usersmanagement.infrastructure.security.JwtAuthenticationFilter;
import com.jcaa.usersmanagement.infrastructure.security.JwtTokenService;
import com.jcaa.usersmanagement.infrastructure.security.RestAccessDeniedHandler;
import com.jcaa.usersmanagement.infrastructure.security.RestAuthenticationEntryPoint;
import com.jcaa.usersmanagement.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica que las excepciones del framework se traduzcan a su codigo HTTP correcto.
 *
 * <p>Estos casos eran un bug: sin handlers especificos, tanto un JSON malformado como una ruta
 * inexistente caian en el catch-all y devolvian 500.
 */
@WebMvcTest(
    controllers = {
      com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller.UserRestController.class,
      com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller.AuthRestController.class
    },
    properties = "spring.main.web-application-type=servlet")
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  RestAuthenticationEntryPoint.class,
  RestAccessDeniedHandler.class,
  GlobalExceptionHandler.class
})
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private CreateUserUseCase createUserUseCase;
  @MockBean private UpdateUserUseCase updateUserUseCase;
  @MockBean private DeleteUserUseCase deleteUserUseCase;
  @MockBean private GetUserByIdUseCase getUserByIdUseCase;
  @MockBean private GetAllUsersUseCase getAllUsersUseCase;
  @MockBean private com.jcaa.usersmanagement.application.port.in.LoginUseCase loginUseCase;
  @MockBean private JwtTokenService jwtTokenService;

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Un cuerpo que no es JSON valido debe devolver 400 y no 500")
  void shouldReturnBadRequestForMalformedJson() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{esto no es json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("El cuerpo de la peticion no es un JSON valido."));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Un campo que espera texto pero trae un objeto debe devolver 400 y no 500")
  void shouldReturnBadRequestForWrongFieldType() throws Exception {
    // Jackson no puede convertir un objeto a String: lanza MismatchedInputException, que Spring
    // envuelve en HttpMessageNotReadableException. Sin el handler especifico terminaba en 500.
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\": {\"no\": \"es texto\"}, \"email\": \"a@b.co\","
                        + " \"password\": \"Secure123!\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Un numero donde se espera texto lo coerciona Jackson y pasa la deserializacion")
  void shouldCoerceNumberToStringAndNotFail() throws Exception {
    // Jackson convierte 12345 en "12345". Documenta ese comportamiento: no es un error de tipo,
    // asi que la peticion avanza a la validacion (@Size min 3) y 12345 la cumple.
    // El UseCase esta mockeado a null, por lo que el controlador falla al mapear la respuesta.
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": 12345, \"email\": \"a@b.co\", \"password\": \"Secure123!\"}"))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(400));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Una ruta inexistente debe devolver 404 y no 500")
  void shouldReturnNotFoundForUnknownRoute() throws Exception {
    mockMvc
        .perform(get("/api/esteRecursoNoExiste"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Un metodo HTTP no soportado debe devolver 405 y no 500")
  void shouldReturnMethodNotAllowed() throws Exception {
    mockMvc
        .perform(patch("/api/users"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.status").value(405));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Un Content-Type no soportado debe devolver 415")
  void shouldReturnUnsupportedMediaType() throws Exception {
    mockMvc
        .perform(post("/api/auth/register").contentType(MediaType.TEXT_PLAIN).content("texto plano"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.status").value(415));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("DELETE sobre un recurso sin endpoint debe devolver 405 y no 500")
  void shouldReturnMethodNotAllowedForDeleteOnCollection() throws Exception {
    mockMvc
        .perform(delete("/api/users"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.status").value(HttpStatus.METHOD_NOT_ALLOWED.value()));
  }
}