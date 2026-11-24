package com.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.model.PersonalAccount;
import com.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
class PersonalAccountServiceTest {
  @Mock
  private AccountRepository accountRepository;

  @InjectMocks
  private PersonalAccountService personalAccountService;

  @Test
  void createsAndSavesPersonalAccount() {
    when(accountRepository.save(any(PersonalAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PersonalAccount account = personalAccountService.createPersonalAccount(
        "octocat@github.com", "secure-password");

    assertEquals("octocat@github.com", account.getEmail());
    assertTrue(account.matchesPassword("secure-password"));
    verify(accountRepository).save(any(PersonalAccount.class));
  }

  @Test
  void getsPersonalAccountOwnedByRequester() {
    PersonalAccount requestingAccount = new PersonalAccount("octocat@github.com", "secure-password");
    requestingAccount.setId("account-1");
    when(accountRepository.findById("account-1")).thenReturn(Optional.of(requestingAccount));

    PersonalAccount account = personalAccountService.getPersonalAccount(requestingAccount, "account-1");

    assertSame(requestingAccount, account);
    verify(accountRepository).findById("account-1");
  }

  @Test
  void rejectsRequestForAnotherAccountsId() {
    PersonalAccount requestingAccount = new PersonalAccount("octocat@github.com", "secure-password");
    requestingAccount.setId("account-1");

    assertThrows(SecurityException.class,
        () -> personalAccountService.getPersonalAccount(requestingAccount, "account-2"));

    verify(accountRepository, never()).findById(any());
  }

  @Test
  void updatesOwnedPersonalAccount() {
    PersonalAccount requestingAccount = new PersonalAccount("octocat@github.com", "secure-password");
    requestingAccount.setId("account-1");
    PersonalAccount storedAccount = new PersonalAccount("octocat@github.com", "secure-password");
    storedAccount.setId("account-1");
    PersonalAccount updatedAccount = new PersonalAccount("hubot@github.com", "new-password");
    when(accountRepository.findById("account-1")).thenReturn(Optional.of(storedAccount));
    when(accountRepository.save(storedAccount)).thenReturn(storedAccount);

    PersonalAccount result = personalAccountService.updatePersonalAccount(
        requestingAccount, "account-1", updatedAccount);

    assertEquals("hubot@github.com", result.getEmail());
    assertTrue(result.matchesPassword("new-password"));
    verify(accountRepository).save(storedAccount);
  }

  @Test
  void rejectsUpdatingAnotherAccountsId() {
    PersonalAccount requestingAccount = new PersonalAccount("octocat@github.com", "secure-password");
    requestingAccount.setId("account-1");

    assertThrows(SecurityException.class,
        () -> personalAccountService.updatePersonalAccount(
            requestingAccount, "account-2", new PersonalAccount("hubot@github.com", "new-password")));

    verify(accountRepository, never()).save(any(PersonalAccount.class));
  }

  @Test
  void deletesOwnedPersonalAccount() {
    PersonalAccount requestingAccount = new PersonalAccount("octocat@github.com", "secure-password");
    requestingAccount.setId("account-1");
    when(accountRepository.findById("account-1")).thenReturn(Optional.of(requestingAccount));

    personalAccountService.deletePersonalAccount(requestingAccount, "account-1");

    verify(accountRepository).delete(requestingAccount);
  }

  @Test
  void rejectsDeletingAnotherAccountsId() {
    PersonalAccount requestingAccount = new PersonalAccount("octocat@github.com", "secure-password");
    requestingAccount.setId("account-1");

    assertThrows(SecurityException.class,
        () -> personalAccountService.deletePersonalAccount(requestingAccount, "account-2"));

    verify(accountRepository, never()).delete(any(PersonalAccount.class));
  }
}
