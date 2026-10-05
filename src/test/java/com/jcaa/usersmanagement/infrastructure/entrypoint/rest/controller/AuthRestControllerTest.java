package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.application.port.in.CreateUserUseCase;
import com.jcaa.usersmanagement.application.port.in.LoginUseCase;
import com.jcaa.usersmanagement.application.service.dto.command.CreateUserCommand;
import com.jcaa.usersmanagement.application.service.dto.command.LoginCommand;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request.LoginRestRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request.RegisterRestRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.LoginRestResponse;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.UserRestResponse;
import com.jcaa.usersmanagement.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthRestControllerTest {

  private static final String EMAIL = "test@example.com";
  private static final String PASSWORD = "SecurePass1";

  @Mock private LoginUseCase loginUseCase;
  @Mock private CreateUserUseCase createUserUseCase;
  @Mock private JwtTokenService jwtTokenService;

  @Test
  void shouldAuthenticateAndReturnBearerToken() {
    // Arrange
    final UserModel user = activeUser();
    final LoginCommand command = new LoginCommand(EMAIL, PASSWORD);
    when(loginUseCase.execute(command)).thenReturn(user);
    when(jwtTokenService.generate(user)).thenReturn("signed.jwt.token");
    when(jwtTokenService.expirationSeconds()).thenReturn(900L);
    final AuthRestController controller =
        new AuthRestController(loginUseCase, createUserUseCase, jwtTokenService);

    // Act
    final LoginRestResponse response =
        controller.login(new LoginRestRequest(EMAIL, PASSWORD));

    // Assert
    assertThat(response.accessToken()).isEqualTo("signed.jwt.token");
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.expiresIn()).isEqualTo(900L);
    verify(loginUseCase).execute(command);
  }

  @Test
  void shouldForceMemberRoleOnPublicRegistration() {
    // Arrange
    final UserModel registered = pendingMember();
    when(createUserUseCase.execute(any(CreateUserCommand.class))).thenReturn(registered);
    final AuthRestController controller =
        new AuthRestController(loginUseCase, createUserUseCase, jwtTokenService);

    // Act
    final UserRestResponse response =
        controller.register(new RegisterRestRequest("Nuevo Usuario", "nuevo@example.com", "SecurePass1"));

    // Assert
    final ArgumentCaptor<CreateUserCommand> captor = ArgumentCaptor.forClass(CreateUserCommand.class);
    verify(createUserUseCase).execute(captor.capture());
    final CreateUserCommand command = captor.getValue();

    assertThat(command.role())
        .as("el registro público nunca debe poder elegir un rol privilegiado")
        .isEqualTo("MEMBER");
    assertThat(command.id())
        .as("el ID debe generarlo el servidor")
        .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    assertThat(command.email()).isEqualTo("nuevo@example.com");
    assertThat(response.role()).isEqualTo("MEMBER");
    assertThat(response.status()).isEqualTo("PENDING");
  }

  private static UserModel activeUser() {
    return new UserModel(
        new UserId("user-001"),
        new UserName("Test User"),
        new UserEmail(EMAIL),
        UserPassword.fromPlainText(PASSWORD),
        UserRole.ADMIN,
        UserStatus.ACTIVE);
  }

  private static UserModel pendingMember() {
    return UserModel.create(
        new UserId("user-002"),
        new UserName("Nuevo Usuario"),
        new UserEmail("nuevo@example.com"),
        UserPassword.fromPlainText(PASSWORD),
        UserRole.MEMBER);
  }
}
