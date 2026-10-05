package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response;

public record LoginRestResponse(
    String accessToken,
    String tokenType,
    long expiresIn) {}
