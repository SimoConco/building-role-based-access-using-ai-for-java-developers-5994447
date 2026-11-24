package com.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.model.PersonalAccount;
import com.security.JwtService;
import com.service.PersonalAccountService;

@RestController
@RequestMapping("/api")
public class PersonalAccountController {
  private final PersonalAccountService personalAccountService;
  private final JwtService jwtService;

  public PersonalAccountController(
      PersonalAccountService personalAccountService, JwtService jwtService) {
    this.personalAccountService = personalAccountService;
    this.jwtService = jwtService;
  }

  @PostMapping("/auth/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    PersonalAccount account = personalAccountService.createPersonalAccount(
        request.email(), request.password());
    return ResponseEntity.status(HttpStatus.CREATED).body(toAuthResponse(account));
  }

  @PostMapping("/auth/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return toAuthResponse(personalAccountService.authenticate(request.email(), request.password()));
  }

  @GetMapping("/accounts/me")
  public AccountResponse getCurrentAccount(@AuthenticationPrincipal PersonalAccount account) {
    return toAccountResponse(personalAccountService.getPersonalAccount(account, account.getId()));
  }

  @PatchMapping("/accounts/me")
  public AccountResponse updateCurrentAccount(
      @AuthenticationPrincipal PersonalAccount account,
      @Valid @RequestBody UpdateAccountRequest request) {
    PersonalAccount updated = personalAccountService.updatePersonalAccount(
        account, account.getId(), request.email(), request.password());
    return toAccountResponse(updated);
  }

  @DeleteMapping("/accounts/me")
  public ResponseEntity<Void> deleteCurrentAccount(
      @AuthenticationPrincipal PersonalAccount account) {
    personalAccountService.deletePersonalAccount(account, account.getId());
    return ResponseEntity.noContent().build();
  }

  private AuthResponse toAuthResponse(PersonalAccount account) {
    return new AuthResponse(account.getId(), account.getEmail(), jwtService.generateToken(account));
  }

  private AccountResponse toAccountResponse(PersonalAccount account) {
    return new AccountResponse(account.getId(), account.getEmail());
  }

  public record RegisterRequest(
      @NotBlank @Email String email,
      @NotBlank @Size(min = 8, max = 72) String password) {
  }

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
  }

  public record UpdateAccountRequest(
      @Email String email,
      @Size(min = 8, max = 72) String password) {
  }

  public record AuthResponse(String accountId, String email, String token) {
  }

  public record AccountResponse(String accountId, String email) {
  }
}
