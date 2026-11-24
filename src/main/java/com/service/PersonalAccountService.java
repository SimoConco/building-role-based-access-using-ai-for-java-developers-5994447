package com.service;

import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.exceptions.AccountNotFoundException;
import com.exceptions.EmailAlreadyExistsException;
import com.exceptions.InvalidCredentialsException;
import com.model.PersonalAccount;
import com.repository.AccountRepository;

@Service
public class PersonalAccountService {
  private final AccountRepository accountRepository;

  public PersonalAccountService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  public PersonalAccount createPersonalAccount(String email, String password) {
    if (accountRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException(email);
    }
    return accountRepository.save(new PersonalAccount(email, password));
  }

  public PersonalAccount authenticate(String email, String password) {
    return accountRepository.findByEmail(email)
        .filter(account -> account.matchesPassword(password))
        .orElseThrow(InvalidCredentialsException::new);
  }

  public PersonalAccount getPersonalAccount(PersonalAccount requestingAccount, String accountId) {
    validateOwnership(requestingAccount, accountId);
    return accountRepository.findById(accountId)
        .orElseThrow(() -> new AccountNotFoundException(accountId));
  }

  public PersonalAccount updatePersonalAccount(
      PersonalAccount requestingAccount, String accountId, PersonalAccount updatedAccount) {
    if (updatedAccount == null) {
      throw new IllegalArgumentException("Updated personal account must not be null");
    }

    PersonalAccount account = getPersonalAccount(requestingAccount, accountId);
    account.setEmail(updatedAccount.getEmail());
    if (updatedAccount.getPasswordHash() != null) {
      account.setPasswordHash(updatedAccount.getPasswordHash());
    }
    account.setUpdatedAt(Instant.now());
    return accountRepository.save(account);
  }

  public PersonalAccount updatePersonalAccount(
      PersonalAccount requestingAccount, String accountId, String email, String password) {
    PersonalAccount account = getPersonalAccount(requestingAccount, accountId);
    if (email != null && !email.equals(account.getEmail())) {
      if (accountRepository.existsByEmail(email)) {
        throw new EmailAlreadyExistsException(email);
      }
      account.setEmail(email);
    }
    if (password != null) {
      account.setPassword(password);
    }
    account.setUpdatedAt(Instant.now());
    return accountRepository.save(account);
  }

  public void deletePersonalAccount(PersonalAccount requestingAccount, String accountId) {
    PersonalAccount account = getPersonalAccount(requestingAccount, accountId);
    accountRepository.delete(account);
  }

  private void validateOwnership(PersonalAccount requestingAccount, String accountId) {
    if (requestingAccount == null || !Objects.equals(requestingAccount.getId(), accountId)) {
      throw new SecurityException("A personal account can only access its own account");
    }
  }
}
