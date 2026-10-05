package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import com.jcaa.usersmanagement.application.port.in.CreateUserUseCase;
import com.jcaa.usersmanagement.application.port.in.LoginUseCase;
import com.jcaa.usersmanagement.application.service.dto.command.LoginCommand;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request.LoginRestRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request.RegisterRestRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.LoginRestResponse;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.UserRestResponse;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.mapper.UserRestMapper;
import com.jcaa.usersmanagement.infrastructure.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

  private static final String BEARER_TOKEN_TYPE = "Bearer";

  private final LoginUseCase loginUseCase;
  private final CreateUserUseCase createUserUseCase;
  private final JwtTokenService jwtTokenService;

  @PostMapping("/login")
  @Operation(summary = "Autenticar usuario y obtener un token JWT")
  public LoginRestResponse login(@Valid @RequestBody final LoginRestRequest request) {
    final UserModel user = loginUseCase.execute(new LoginCommand(request.email(), request.password()));
    return new LoginRestResponse(
        jwtTokenService.generate(user), BEARER_TOKEN_TYPE, jwtTokenService.expirationSeconds());
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Registrar un usuario público",
      description =
          "Crea una cuenta nueva sin necesidad de autenticación. "
              + "El rol se fija en MEMBER y el usuario queda en estado PENDING, "
              + "por lo que un ADMIN debe activarlo antes del primer inicio de sesión.")
  public UserRestResponse register(@Valid @RequestBody final RegisterRestRequest request) {
    final UserModel user = createUserUseCase.execute(UserRestMapper.toRegisterCommand(request));
    return UserRestMapper.toResponse(user);
  }
}
