package com.jcaa.usersmanagement.infrastructure.security;

import com.jcaa.usersmanagement.domain.exception.DomainException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String ROLE_PREFIX = "ROLE_";

  private final JwtTokenService jwtTokenService;

  public JwtAuthenticationFilter(final JwtTokenService jwtTokenService) {
    this.jwtTokenService = jwtTokenService;
  }

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {
    final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (hasBearerToken(authorization)) {
      authenticate(authorization.substring(BEARER_PREFIX.length()));
    }
    filterChain.doFilter(request, response);
  }

  private static boolean hasBearerToken(final String authorization) {
    return StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX);
  }

  private void authenticate(final String token) {
    try {
      final JwtPrincipal principal = jwtTokenService.parse(token);
      final SimpleGrantedAuthority authority =
          new SimpleGrantedAuthority(ROLE_PREFIX + principal.role().name());
      final UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(principal.userId(), null, List.of(authority));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (final JwtException | DomainException | IllegalArgumentException ignored) {
      SecurityContextHolder.clearContext();
    }
  }
}
