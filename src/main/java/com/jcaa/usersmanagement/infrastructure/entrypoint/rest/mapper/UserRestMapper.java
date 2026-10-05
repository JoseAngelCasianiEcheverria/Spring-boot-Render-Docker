package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.mapper;

import com.jcaa.usersmanagement.application.service.dto.command.CreateUserCommand;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteUserCommand;
import com.jcaa.usersmanagement.application.service.dto.command.UpdateUserCommand;
import com.jcaa.usersmanagement.application.service.dto.query.GetUserByIdQuery;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request.CreateUserRestRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request.RegisterRestRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request.UpdateUserRestRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.UserRestResponse;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.UUID;

@UtilityClass
public class UserRestMapper {

  /**
   * Rol que se asigna al registrarse públicamente. Un usuario anónimo nunca puede elegir otro.
   */
  public static final String DEFAULT_REGISTER_ROLE = "MEMBER";

  public CreateUserCommand toCreateCommand(final CreateUserRestRequest request) {
    return new CreateUserCommand(
        newUserId(),
        request.name(),
        request.email(),
        request.password(),
        request.role());
  }

  /**
   * Traduce una solicitud de registro público. El identificador se genera en el servidor y el rol
   * se fuerza a {@link #DEFAULT_REGISTER_ROLE}, ignorando cualquier valor que llegue en el cuerpo.
   */
  public CreateUserCommand toRegisterCommand(final RegisterRestRequest request) {
    return new CreateUserCommand(
        newUserId(), request.name(), request.email(), request.password(), DEFAULT_REGISTER_ROLE);
  }

  public UpdateUserCommand toUpdateCommand(final String id, final UpdateUserRestRequest request) {
    return new UpdateUserCommand(
        id,
        request.name(),
        request.email(),
        request.password(),
        request.role(),
        request.status());
  }

  public GetUserByIdQuery toGetByIdQuery(final String id) {
    return new GetUserByIdQuery(id);
  }

  public DeleteUserCommand toDeleteCommand(final String id) {
    return new DeleteUserCommand(id);
  }

  public UserRestResponse toResponse(final UserModel user) {
    return new UserRestResponse(
        user.getId().value(),
        user.getName().value(),
        user.getEmail().value(),
        user.getRole().name(),
        user.getStatus().name());
  }

  public List<UserRestResponse> toResponseList(final List<UserModel> users) {
    return users.stream().map(UserRestMapper::toResponse).toList();
  }

  private String newUserId() {
    return UUID.randomUUID().toString();
  }
}

