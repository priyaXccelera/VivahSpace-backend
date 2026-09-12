package com.example.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String HEADER = "Authorization";
  private static final String PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final CustomUserDetailsService userDetailsService;
  private final RequestMatcher skipMatcher;

  public JwtAuthenticationFilter(
      JwtService jwtService, CustomUserDetailsService userDetailsService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
    this.skipMatcher =
        new org.springframework.security.web.util.matcher.OrRequestMatcher(
            List.of(
                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                    "/api/v1/auth/**"),
                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                    "/actuator/**"),
                new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/docs/**"),
                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                    "/api-docs/**"),
                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                    "/swagger-ui/**"),
                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                    "/api/v1/users/*/exists")));
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    return skipMatcher.matches(request);
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HEADER);

    if (header == null || !header.startsWith(PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = header.substring(PREFIX.length());

    if (jwtService.isTokenParsable(token)
        && SecurityContextHolder.getContext().getAuthentication() == null) {
      String email = jwtService.extractEmail(token);
      UserDetails userDetails = userDetailsService.loadUserByUsername(email);

      if (jwtService.isTokenValid(token, userDetails.getUsername()) && userDetails.isEnabled()) {
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }

    filterChain.doFilter(request, response);
  }
}
