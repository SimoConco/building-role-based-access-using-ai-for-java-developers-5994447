package com.exceptions;

public class AccountNotFoundException extends RuntimeException {
  public AccountNotFoundException(String accountId) {
    super("Personal account not found: " + accountId);
  }
}
