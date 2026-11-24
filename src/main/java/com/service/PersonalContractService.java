package com.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.exceptions.AccountNotFoundException;
import com.exceptions.ContractNotFoundException;
import com.model.Contract;
import com.model.PersonalAccount;
import com.repository.AccountRepository;
import com.repository.ContractRepository;

@Service
public class PersonalContractService {
  private final ContractRepository contractRepository;
  private final AccountRepository accountRepository;

  public PersonalContractService(ContractRepository contractRepository, AccountRepository accountRepository) {
    this.contractRepository = contractRepository;
    this.accountRepository = accountRepository;
  }

  public Contract createContract(PersonalAccount requestingAccount, String name, String description) {
    PersonalAccount account = requireAccount(requestingAccount);
    return contractRepository.save(new Contract(name, description, account));
  }

  public Contract getContract(PersonalAccount requestingAccount, String contractId) {
    PersonalAccount account = requireAccount(requestingAccount);
    Contract contract = contractRepository.findById(contractId)
        .orElseThrow(() -> new ContractNotFoundException(contractId));
    validateOwnership(account, contract);
    return contract;
  }

  public Contract updateContract(
      PersonalAccount requestingAccount, String contractId, String name, String description) {
    Contract contract = getContract(requestingAccount, contractId);
    contract.update(name, description);
    return contractRepository.save(contract);
  }

  public void deleteContract(PersonalAccount requestingAccount, String contractId) {
    contractRepository.delete(getContract(requestingAccount, contractId));
  }

  private PersonalAccount requireAccount(PersonalAccount requestingAccount) {
    if (requestingAccount == null || requestingAccount.getId() == null) {
      throw new AccountNotFoundException(null);
    }
    return accountRepository.findById(requestingAccount.getId())
        .orElseThrow(() -> new AccountNotFoundException(requestingAccount.getId()));
  }

  private void validateOwnership(PersonalAccount requestingAccount, Contract contract) {
    if (contract.getOwner() == null
        || !Objects.equals(contract.getOwner().getId(), requestingAccount.getId())) {
      throw new SecurityException("A personal account can only access its own contracts");
    }
  }
}
