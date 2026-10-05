package com.jcaa.usersmanagement.infrastructure.security;

import com.jcaa.usersmanagement.domain.enums.UserRole;

public record JwtPrincipal(String userId, UserRole role) {}
