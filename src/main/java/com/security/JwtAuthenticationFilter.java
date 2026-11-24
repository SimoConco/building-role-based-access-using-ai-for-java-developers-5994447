package com.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import com.model.PersonalAccount;
import com.repository.AccountRepository;

import io.jsonwebtoken.JwtException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final AccountRepository accountRepository;

  public JwtAuthenticationFilter(JwtService jwtService, AccountRepository accountRepository) {
    this.jwtService = jwtService;
    this.accountRepository = accountRepository;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String authorization = request.getHeader("Authorization");
    if (authorization != null && authorization.startsWith(BEARER_PREFIX)
        && SecurityContextHolder.getContext().getAuthentication() == null) {
      authenticate(authorization.substring(BEARER_PREFIX.length()));
    }

    filterChain.doFilter(request, response);
  }

  private void authenticate(String token) {
    try {
      String accountId = jwtService.getAccountIdFromToken(token);
      Optional<PersonalAccount> account = accountRepository.findById(accountId);
      if (account.filter(value -> jwtService.isTokenValid(token, value)).isPresent()) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(account.get(), null, List.of()));
      }
      
    } catch (JwtException | IllegalArgumentException ignored) {
      SecurityContextHolder.clearContext();
    }
  }
}
