package com.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.model.PersonalAccount;

class JwtServiceTest {
  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    String testSecret = Base64.getEncoder().encodeToString(
        "test-signing-key-with-at-least-32-bytes".getBytes());
    jwtService = new JwtService(testSecret, 60_000);
  }

  @Test
  void generatesTokenForPersonalAccount() {
    PersonalAccount account = account("account-1");

    String token = jwtService.generateToken(account);

    assertEquals("account-1", jwtService.getAccountIdFromToken(token));
    assertTrue(jwtService.isTokenValid(token, account));
  }

  @Test
  void rejectsTokenForDifferentAccount() {
    String token = jwtService.generateToken(account("account-1"));

    assertFalse(jwtService.isTokenValid(token, account("account-2")));
  }

  @Test
  void rejectsMalformedToken() {
    assertFalse(jwtService.isTokenValid("not-a-jwt", account("account-1")));
  }

  @Test
  void refusesToGenerateTokenWithoutAccountId() {
    assertThrows(IllegalArgumentException.class,
        () -> jwtService.generateToken(new PersonalAccount("octocat@github.com", "password")));
  }

  private PersonalAccount account(String id) {
    PersonalAccount account = new PersonalAccount("octocat@github.com", "password");
    account.setId(id);
    return account;
  }
}
