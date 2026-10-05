package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response;

public record UserRestResponse(
    String id,
    String name,
    String email,
    String role,
    String status) {}

