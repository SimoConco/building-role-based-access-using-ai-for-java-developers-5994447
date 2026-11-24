package com.security;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.model.PersonalAccount;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
  private final SecretKey signingKey;
  private final long expirationMillis;

  public JwtService(
      @Value("${security.jwt.secret}") String base64Secret,
      @Value("${security.jwt.expiration-ms}") long expirationMillis) {
    this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
    if (expirationMillis <= 0) {
      throw new IllegalArgumentException("JWT expiration must be positive");
    }
    this.expirationMillis = expirationMillis;
  }

  public String generateToken(PersonalAccount account) {
    if (account == null || account.getId() == null || account.getId().isBlank()) {
      throw new IllegalArgumentException("Personal account must have an ID");
    }

    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plusMillis(expirationMillis);
    return Jwts.builder()
        .subject(account.getId())
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiresAt))
        .signWith(signingKey)
        .compact();
  }

  public String getAccountIdFromToken(String token) {
    return parseClaims(token).getSubject();
  }

  public boolean isTokenValid(String token, PersonalAccount account) {
    if (account == null || account.getId() == null) {
      return false;
    }
    try {
      return Objects.equals(parseClaims(token).getSubject(), account.getId());
    } catch (JwtException | IllegalArgumentException exception) {
      return false;
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(signingKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
