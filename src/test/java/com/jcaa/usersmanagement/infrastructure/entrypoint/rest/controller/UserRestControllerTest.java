package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jcaa.usersmanagement.application.port.in.CreateUserUseCase;
import com.jcaa.usersmanagement.application.port.in.DeleteUserUseCase;
import com.jcaa.usersmanagement.application.port.in.GetAllUsersUseCase;
import com.jcaa.usersmanagement.application.port.in.GetUserByIdUseCase;
import com.jcaa.usersmanagement.application.port.in.UpdateUserUseCase;
import com.jcaa.usersmanagement.application.service.dto.command.CreateUserCommand;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteUserCommand;
import com.jcaa.usersmanagement.application.service.dto.command.UpdateUserCommand;
import com.jcaa.usersmanagement.application.service.dto.query.GetUserByIdQuery;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import com.jcaa.usersmanagement.infrastructure.security.JwtAuthenticationFilter;
import com.jcaa.usersmanagement.infrastructure.security.JwtPrincipal;
import com.jcaa.usersmanagement.infrastructure.security.JwtTokenService;
import com.jcaa.usersmanagement.infrastructure.security.RestAccessDeniedHandler;
import com.jcaa.usersmanagement.infrastructure.security.RestAuthenticationEntryPoint;
import com.jcaa.usersmanagement.infrastructure.security.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = UserRestController.class,
    properties = "spring.main.web-application-type=servlet")
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  RestAuthenticationEntryPoint.class,
  RestAccessDeniedHandler.class
})
@DisplayName("UserRestController")
class UserRestControllerTest {

  private static final String ID = "8f684dc4-0c67-4813-8d58-7b5e7f7937b8";

  @Autowired private MockMvc mockMvc;

  @MockBean private CreateUserUseCase createUserUseCase;
  @MockBean private UpdateUserUseCase updateUserUseCase;
  @MockBean private DeleteUserUseCase deleteUserUseCase;
  @MockBean private GetUserByIdUseCase getUserByIdUseCase;
  @MockBean private GetAllUsersUseCase getAllUsersUseCase;
  @MockBean private JwtTokenService jwtTokenService;

  @Test
  @DisplayName("GET /api/users debe retornar todos los usuarios")
  void shouldGetAllUsersThroughSpringMvc() throws Exception {
    final UserModel user =
        new UserModel(
            new UserId(ID),
            new UserName("Ada Lovelace"),
            new UserEmail("ada@example.com"),
            UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
            UserRole.ADMIN,
            UserStatus.ACTIVE);
    when(getAllUsersUseCase.execute()).thenReturn(List.of(user));
    when(jwtTokenService.parse("valid-token")).thenReturn(new JwtPrincipal(ID, UserRole.REVIEWER));

    mockMvc
        .perform(get("/api/users").header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(ID))
        .andExpect(jsonPath("$[0].name").value("Ada Lovelace"))
        .andExpect(jsonPath("$[0].email").value("ada@example.com"))
        .andExpect(jsonPath("$[0].role").value("ADMIN"))
        .andExpect(jsonPath("$[0].status").value("ACTIVE"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("POST /api/users debe crear un usuario válido con el ID generado en el servidor")
  void shouldCreateUserThroughSpringMvc() throws Exception {
    final String request =
        """
        {
          "name": "Ada Lovelace",
          "email": "ada@example.com",
          "password": "Secure123!",
          "role": "ADMIN"
        }
        """;
    final UserModel user =
        new UserModel(
            new UserId(ID),
            new UserName("Ada Lovelace"),
            new UserEmail("ada@example.com"),
            UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
            UserRole.ADMIN,
            UserStatus.ACTIVE);
    when(createUserUseCase.execute(any(CreateUserCommand.class))).thenReturn(user);

    mockMvc
        .perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(ID));

    final ArgumentCaptor<CreateUserCommand> captor = ArgumentCaptor.forClass(CreateUserCommand.class);
    verify(createUserUseCase).execute(captor.capture());
    final CreateUserCommand command = captor.getValue();
    assertThat(command.id())
        .as("el ID debe generarlo el servidor como UUID, no venir del cuerpo")
        .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    assertThat(command.role()).isEqualTo("ADMIN");
    assertThat(command.email()).isEqualTo("ada@example.com");
  }

  @Test
  @DisplayName("POST /api/users debe exigir autenticacion")
  void shouldRejectUnauthenticatedCreateRequest() throws Exception {
    final String request =
        """
        {
          "name": "Intruso",
          "email": "intruso@example.com",
          "password": "Secure123!",
          "role": "ADMIN"
        }
        """;

    mockMvc
        .perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));

    verifyNoInteractions(createUserUseCase);
  }

  @Test
  @WithMockUser(roles = "MEMBER")
  @DisplayName("POST /api/users debe rechazar a un MEMBER aunque se autoasigne ADMIN")
  void shouldRejectMemberCreatingAdmin() throws Exception {
    final String request =
        """
        {
          "name": "Intruso",
          "email": "intruso@example.com",
          "password": "Secure123!",
          "role": "ADMIN"
        }
        """;

    mockMvc
        .perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));

    verifyNoInteractions(createUserUseCase);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PUT /api/users/{id} debe actualizar un usuario")
  void shouldUpdateUserThroughSpringMvc() throws Exception {
    final String request =
        """
        {
          "name": "Ada Byron",
          "email": "ada.byron@example.com",
          "password": "NewSecure123!",
          "role": "REVIEWER",
          "status": "ACTIVE"
        }
        """;
    final UserModel user =
        new UserModel(
            new UserId(ID),
            new UserName("Ada Byron"),
            new UserEmail("ada.byron@example.com"),
            UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
            UserRole.REVIEWER,
            UserStatus.ACTIVE);
    when(updateUserUseCase.execute(
            new UpdateUserCommand(
                ID,
                "Ada Byron",
                "ada.byron@example.com",
                "NewSecure123!",
                "REVIEWER",
                "ACTIVE")))
        .thenReturn(user);

    mockMvc
        .perform(put("/api/users/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Ada Byron"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("DELETE /api/users/{id} debe eliminar un usuario")
  void shouldDeleteUserThroughSpringMvc() throws Exception {
    mockMvc.perform(delete("/api/users/{id}", ID)).andExpect(status().isNoContent());

    verify(deleteUserUseCase).execute(new DeleteUserCommand(ID));
  }

  @Test
  @WithMockUser(roles = "REVIEWER")
  @DisplayName("GET /api/users/{id} debe responder usando el caso de uso inyectado por Spring")
  void shouldGetUserByIdThroughSpringMvc() throws Exception {
    // Arrange
    final UserModel user =
        new UserModel(
            new UserId(ID),
            new UserName("Ada Lovelace"),
            new UserEmail("ada@example.com"),
            UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
            UserRole.ADMIN,
            UserStatus.ACTIVE);
    when(getUserByIdUseCase.execute(new GetUserByIdQuery(ID))).thenReturn(user);

    // Act & Assert
    mockMvc
        .perform(get("/api/users/{id}", ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ID))
        .andExpect(jsonPath("$.name").value("Ada Lovelace"))
        .andExpect(jsonPath("$.email").value("ada@example.com"))
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("POST /api/users debe rechazar una solicitud inválida antes del caso de uso")
  void shouldRejectInvalidCreateRequest() throws Exception {
    // Arrange
    final String invalidRequest =
        """
        {
          "name": "A",
          "email": "invalid",
          "password": "short",
          "role": ""
        }
        """;

    // Act & Assert
    mockMvc
        .perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(invalidRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  @DisplayName("GET /api/users/{id} debe requerir autenticacion")
  void shouldRejectUnauthenticatedRequest() throws Exception {
    // Arrange

    // Act & Assert
    mockMvc
        .perform(get("/api/users/{id}", ID))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  @WithMockUser(roles = "MEMBER")
  @DisplayName("GET /api/users/{id} debe rechazar un rol sin permisos")
  void shouldRejectUnauthorizedRole() throws Exception {
    // Arrange

    // Act & Assert
    mockMvc
        .perform(get("/api/users/{id}", ID))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }
}
