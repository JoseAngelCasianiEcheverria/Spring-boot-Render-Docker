package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de registro público.
 *
 * <p>A diferencia de {@link CreateUserRestRequest}, este contrato no expone {@code id} ni
 * {@code role}: el identificador se genera en el servidor y el rol se fuerza a {@code MEMBER}.
 * Esto impide que un usuario anónimo se autoasigne un rol privilegiado.
 */
public record RegisterRestRequest(
    @NotBlank(message = "name must not be blank")
        @Size(min = 3, message = "name must have at least 3 characters")
        String name,
    @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email,
    @NotBlank(message = "password must not be blank")
        @Size(min = 8, message = "password must have at least 8 characters")
        String password) {}