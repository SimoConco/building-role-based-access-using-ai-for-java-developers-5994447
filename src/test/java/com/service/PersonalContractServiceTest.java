package com.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.model.Contract;
import com.model.PersonalAccount;
import com.repository.AccountRepository;
import com.repository.ContractRepository;

@ExtendWith(MockitoExtension.class)
class PersonalContractServiceTest {
  @Mock
  private ContractRepository contractRepository;

  @Mock
  private AccountRepository accountRepository;

  @InjectMocks
  private PersonalContractService personalContractService;

  @Test
  void getsOwnedContract() {
    PersonalAccount account = account("account-1");
    Contract contract = new Contract("GitHub Enterprise", "Annual plan", account);
    when(accountRepository.findById("account-1")).thenReturn(Optional.of(account));
    when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));

    Contract result = personalContractService.getContract(account, contract.getId());

    assertSame(contract, result);
  }

  @Test
  void updatesOwnedContract() {
    PersonalAccount account = account("account-1");
    Contract contract = new Contract("GitHub Team", "Monthly plan", account);
    when(accountRepository.findById("account-1")).thenReturn(Optional.of(account));
    when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
    when(contractRepository.save(contract)).thenReturn(contract);

    Contract result = personalContractService.updateContract(
        account, contract.getId(), "GitHub Enterprise", "Annual plan");

    assertSame(contract, result);
    assertEquals("GitHub Enterprise", result.getName());
    assertEquals("Annual plan", result.getDescription());
    verify(contractRepository).save(contract);
  }

  @Test
  void deletesOwnedContract() {
    PersonalAccount account = account("account-1");
    Contract contract = new Contract("GitHub Team", "Monthly plan", account);
    when(accountRepository.findById("account-1")).thenReturn(Optional.of(account));
    when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));

    personalContractService.deleteContract(account, contract.getId());

    verify(contractRepository).delete(contract);
  }

  @Test
  void rejectsAccessToAnotherAccountsContract() {
    PersonalAccount requestingAccount = account("account-1");
    Contract contract = new Contract("GitHub Team", "Monthly plan", account("account-2"));
    when(accountRepository.findById("account-1")).thenReturn(Optional.of(requestingAccount));
    when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));

    assertThrows(SecurityException.class,
        () -> personalContractService.getContract(requestingAccount, contract.getId()));
  }

  private PersonalAccount account(String id) {
    PersonalAccount account = new PersonalAccount("octocat@github.com", "secure-password");
    account.setId(id);
    return account;
  }

  @Test
  void testCreateContract(){
    //Create test account
    PersonalAccount account = new PersonalAccount("octocat@github.com", "secure-password");
    account = accountRepository.save(account);
    when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

    //Create contract
    String contractName = "GitHub Enterprise";
    String contractDescription = "Annual plan";
    Contract contract = personalContractService.createContract(account, contractName, contractDescription);

    //Verify contract is created and saved
    assertNotNull(contract);
    assertEquals(contractName, contract.getName());
    assertEquals(contractDescription, contract.getDescription());
    assertSame(account, contract.getOwner());
    verify(contractRepository).save(contract);
  }

  /**
   * 1. Create a Personal Account for Alice
   * 2. Create a Contract for Alice
   * 3. Validate that contract is created.
   * 4. Update the contract name and description as Alice
   * 5. Validate that contract is updated.
   * 6. Delete the contract as Alice
   * 7. Validate that contract is deleted.
   */

  @Test 
  void testOwnerContractFlow(){
    // Step 1: Create a Personal Account for Alice
    PersonalAccount aliceAccount = new PersonalAccount("alice@github.com", "secure-password");
    aliceAccount = accountRepository.save(aliceAccount);
    when(accountRepository.findById(aliceAccount.getId())).thenReturn(Optional.of(aliceAccount));

    // Step 2: Create a Contract for Alice
    String contractName = "GitHub Enterprise";
    String contractDescription = "Annual plan";
    Contract contract = personalContractService.createContract(aliceAccount, contractName, contractDescription);

    // Step 3: Validate that contract is created
    assertNotNull(contract);
    assertEquals(contractName, contract.getName());
    assertEquals(contractDescription, contract.getDescription());
    assertSame(aliceAccount, contract.getOwner());

    // Step 4: Update the contract name and description as Alice
    String updatedContractName = "GitHub Enterprise Plus";
    String updatedContractDescription = "Annual plan with additional features";
    Contract updatedContract = personalContractService.updateContract(aliceAccount, contract.getId(), updatedContractName, updatedContractDescription);

    // Step 5: Validate that contract is updated
    assertNotNull(updatedContract);
    assertEquals(updatedContractName, updatedContract.getName());
    assertEquals(updatedContractDescription, updatedContract.getDescription());

    // Step 6: Delete the contract as Alice
    personalContractService.deleteContract(aliceAccount, contract.getId());

    // Step 7: Validate that contract is deleted
    verify(contractRepository).delete(contract);
  }
  
  /**
   * 1. Create a Personal Account for Alice
   * 2. Create a Contract for Alice
   * 3. Validate that contract is created.
   * 4. Create a Personal Account for Bob
   * 5. Attempt to update the contract as Bob
   * 6. Validate that a SecurityException is thrown.
   * 7. Attempt to delete the contract as Bob
   * 8. Validate that a SecurityException is thrown.
   */

  @Test 
  void testNonOwnerContractFlow(){
    // Step 1: Create a Personal Account for Alice
    PersonalAccount aliceAccount = new PersonalAccount("alice@github.com", "secure-password");
    aliceAccount = accountRepository.save(aliceAccount);
    when(accountRepository.findById(aliceAccount.getId())).thenReturn(Optional.of(aliceAccount));

    // Step 2: Create a Contract for Alice
    String contractName = "GitHub Enterprise";
    String contractDescription = "Annual plan";
    Contract contract = personalContractService.createContract(aliceAccount, contractName, contractDescription);

    // Step 3: Validate that contract is created
    assertNotNull(contract);
    assertEquals(contractName, contract.getName());
    assertEquals(contractDescription, contract.getDescription());
    assertSame(aliceAccount, contract.getOwner());

    // Step 4: Create a Personal Account for Bob
    PersonalAccount bobAccount = new PersonalAccount("bob@github.com", "secure-password");
    accountRepository.save(bobAccount);
    when(accountRepository.findById(bobAccount.getId())).thenReturn(Optional.of(bobAccount));

    // Step 5: Attempt to update the contract as Bob
    String updatedContractName = "GitHub Enterprise Plus";
    String updatedContractDescription = "Annual plan with additional features";
    assertThrows(SecurityException.class, () -> {
      personalContractService.updateContract(bobAccount, contract.getId(), updatedContractName, updatedContractDescription);
    });

    // Step 7: Attempt to delete the contract as Bob
    assertThrows(SecurityException.class, () -> {
      personalContractService.deleteContract(bobAccount, contract.getId());
    });

  }
}