package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de creación de usuario por parte de un administrador.
 *
 * <p>No expone {@code id}: el identificador se genera en el servidor como UUID. Sí expone {@code
 * role}, porque un administrador sí debe poder asignar el rol legítimamente. El registro público sin
 * privilegios usa {@link RegisterRestRequest}, que no permite elegir rol.
 */
public record CreateUserRestRequest(
    @NotBlank(message = "name must not be blank")
        @Size(min = 3, message = "name must have at least 3 characters")
        String name,
    @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email,
    @NotBlank(message = "password must not be blank")
        @Size(min = 8, message = "password must have at least 8 characters")
        String password,
    @NotBlank(message = "role must not be blank") String role) {}