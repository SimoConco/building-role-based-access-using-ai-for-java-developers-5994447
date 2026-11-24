package com.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ll.App;
import com.model.Contract;
import com.model.PersonalAccount;

@SpringBootTest(classes = App.class)
@ActiveProfiles("test")
class ContractRepositoryTest {
  @Autowired
  private ContractRepository contractRepository;

  @Autowired
  private AccountRepository accountRepository;

  @BeforeEach
  void clearRepositories() {
    contractRepository.deleteAll();
    accountRepository.deleteAll();
  }

  @Test
  void createsContract() {
    PersonalAccount owner = saveAccount("octocat@github.com");
    Contract contract = contractRepository.save(
        new Contract("GitHub Copilot Subscription", "Annual subscription agreement", owner));

    assertNotNull(contract.getId());
    Contract created = contractRepository.findById(contract.getId()).orElseThrow();
    assertEquals("GitHub Copilot Subscription", created.getName());
    assertEquals("Annual subscription agreement", created.getDescription());
    assertEquals(owner.getId(), created.getOwner().getId());
  }

  @Test
  void readsContract() {
    PersonalAccount owner = saveAccount("hubot@github.com");
    Contract contract = contractRepository.save(
        new Contract("GitHub Enterprise", "Enterprise cloud agreement", owner));

    Contract read = contractRepository.findById(contract.getId()).orElseThrow();

    assertEquals(contract.getId(), read.getId());
    assertEquals("GitHub Enterprise", read.getName());
    assertEquals("Enterprise cloud agreement", read.getDescription());
    assertEquals(owner.getId(), read.getOwner().getId());
  }

  @Test
  void updatesContract() {
    PersonalAccount owner = saveAccount("hubot@github.com");
    Contract contract = contractRepository.save(
        new Contract("GitHub Team", "Monthly team plan", owner));

    contract.update("GitHub Enterprise", "Annual enterprise plan");
    contractRepository.save(contract);

    Contract updated = contractRepository.findById(contract.getId()).orElseThrow();
    assertEquals("GitHub Enterprise", updated.getName());
    assertEquals("Annual enterprise plan", updated.getDescription());
    assertEquals(owner.getId(), updated.getOwner().getId());
  }

  @Test
  void deletesContract() {
    PersonalAccount owner = saveAccount("octocat@github.com");
    Contract contract = contractRepository.save(
        new Contract("GitHub Marketplace Agreement", "Marketplace publisher terms", owner));

    contractRepository.deleteById(contract.getId());

    assertFalse(contractRepository.existsById(contract.getId()));
    assertTrue(contractRepository.findById(contract.getId()).isEmpty());
  }

  private PersonalAccount saveAccount(String email) {
    return accountRepository.save(new PersonalAccount(email, "secure-password"));
  }
}
