package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRestRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password) {}
