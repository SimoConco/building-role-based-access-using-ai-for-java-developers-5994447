package com.service;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.exceptions.AccountNotFoundException;
import com.exceptions.ContractNotFoundException;
import com.exceptions.OrganizationNotFoundException;
import com.model.Contract;
import com.model.OrganizationAccount;
import com.model.OrganizationRole;
import com.model.PersonalAccount;
import com.repository.AccountRepository;
import com.repository.ContractRepository;

@Service
public class OrganizationContractService {
  private final ContractRepository contractRepository;
  private final AccountRepository accountRepository;
  private final OrganizationAccountService organizationAccountService;

  @Autowired
  public OrganizationContractService(
      ContractRepository contractRepository,
      AccountRepository accountRepository,
      OrganizationAccountService organizationAccountService) {
    this.contractRepository = contractRepository;
    this.accountRepository = accountRepository;
    this.organizationAccountService = organizationAccountService;
  }

  public OrganizationContractService(
      ContractRepository contractRepository,
      AccountRepository accountRepository) {
    this(contractRepository, accountRepository, null);
  }

  public Contract createContract(
      PersonalAccount requestingAccount,
      String organizationId,
      String name,
      String description) {
    OrganizationAccount organization = requireOrganizationForEditor(
        requestingAccount, organizationId);
    return contractRepository.save(new Contract(name, description, organization));
  }

  public Contract createContract(
      PersonalAccount requestingAccount,
      OrganizationAccount organization,
      String name,
      String description) {
    if (organization == null) {
      throw new OrganizationNotFoundException(null);
    }
    PersonalAccount account = requireAccount(requestingAccount);
    requireMember(account, organization);
    OrganizationRole role = organization.getMemberRole(account.getId());
    if (role != OrganizationRole.owner && role != OrganizationRole.editor) {
      throw new SecurityException(
          "Only organization owners and editors can manage organization contracts");
    }
    return contractRepository.save(new Contract(name, description, organization));
  }

  public Contract createOrganizationContract(
      PersonalAccount requestingAccount,
      String organizationId,
      String name,
      String description) {
    return createContract(requestingAccount, organizationId, name, description);
  }

  public Contract getContract(
      PersonalAccount requestingAccount,
      String organizationId,
      String contractId) {
    PersonalAccount account = requireAccount(requestingAccount);
    OrganizationAccount organization = findOrganization(organizationId);
    requireMember(account, organization);

    Contract contract = findContract(contractId);
    validateContractOwner(contract, organization);
    return contract;
  }

  public Contract getContract(PersonalAccount requestingAccount, String contractId) {
    PersonalAccount account = requireAccount(requestingAccount);
    Contract contract = findContract(contractId);

    if (!(contract.getOwner() instanceof OrganizationAccount organization)) {
      throw new SecurityException("Only organization contracts can be accessed by this service");
    }

    requireMember(account, findOrganization(organization.getId()));
    return contract;
  }

  public Contract getOrganizationContract(
      PersonalAccount requestingAccount,
      String organizationId,
      String contractId) {
    return getContract(requestingAccount, organizationId, contractId);
  }

  public Contract updateContract(
      PersonalAccount requestingAccount,
      String organizationId,
      String contractId,
      String name,
      String description) {
    OrganizationAccount organization = requireOrganizationForEditor(
        requestingAccount, organizationId);
    Contract contract = findContract(contractId);
    validateContractOwner(contract, organization);
    contract.update(name, description);
    return contractRepository.save(contract);
  }

  public Contract updateContract(
      PersonalAccount requestingAccount,
      String contractId,
      String name,
      String description) {
    Contract contract = findContract(contractId);
    OrganizationAccount organization = requireContractOrganization(contract);
    requireOrganizationForEditor(requestingAccount, organization.getId());
    contract.update(name, description);
    return contractRepository.save(contract);
  }

  public Contract updateOrganizationContract(
      PersonalAccount requestingAccount,
      String organizationId,
      String contractId,
      String name,
      String description) {
    return updateContract(requestingAccount, organizationId, contractId, name, description);
  }

  public void deleteContract(
      PersonalAccount requestingAccount,
      String organizationId,
      String contractId) {
    OrganizationAccount organization = requireOrganizationForEditor(
        requestingAccount, organizationId);
    Contract contract = findContract(contractId);
    validateContractOwner(contract, organization);
    contractRepository.delete(contract);
  }

  public void deleteContract(PersonalAccount requestingAccount, String contractId) {
    Contract contract = findContract(contractId);
    OrganizationAccount organization = requireContractOrganization(contract);
    requireOrganizationForEditor(requestingAccount, organization.getId());
    contractRepository.delete(contract);
  }

  public void deleteOrganizationContract(
      PersonalAccount requestingAccount,
      String organizationId,
      String contractId) {
    deleteContract(requestingAccount, organizationId, contractId);
  }

  private PersonalAccount requireAccount(PersonalAccount requestingAccount) {
    if (requestingAccount == null
        || requestingAccount.getId() == null
        || requestingAccount.getId().isBlank()) {
      throw new AccountNotFoundException(
          requestingAccount == null ? null : requestingAccount.getId());
    }
    return accountRepository.findById(requestingAccount.getId())
        .orElseThrow(() -> new AccountNotFoundException(requestingAccount.getId()));
  }

  private OrganizationAccount requireOrganizationForEditor(
      PersonalAccount requestingAccount,
      String organizationId) {
    PersonalAccount account = requireAccount(requestingAccount);
    OrganizationAccount organization = findOrganization(organizationId);
    requireMember(account, organization);

    OrganizationRole role = organization.getMemberRole(account.getId());
    if (role != OrganizationRole.owner && role != OrganizationRole.editor) {
      throw new SecurityException(
          "Only organization owners and editors can manage organization contracts");
    }
    return organization;
  }

  private OrganizationAccount findOrganization(String organizationId) {
    if (organizationId == null || organizationId.isBlank()) {
      throw new OrganizationNotFoundException(organizationId);
    }
    return accountRepository.findOrganizationById(organizationId)
        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
  }

  private void requireMember(
      PersonalAccount account,
      OrganizationAccount organization) {
    if (!organization.isMember(account.getId())) {
      throw new SecurityException("Only organization members can access organization contracts");
    }
  }

  private Contract findContract(String contractId) {
    if (contractId == null || contractId.isBlank()) {
      throw new ContractNotFoundException(contractId);
    }
    return contractRepository.findById(contractId)
        .orElseThrow(() -> new ContractNotFoundException(contractId));
  }

  private OrganizationAccount requireContractOrganization(Contract contract) {
    if (contract.getOwner() instanceof OrganizationAccount organization) {
      return findOrganization(organization.getId());
    }
    throw new SecurityException("The contract is not owned by an organization");
  }

  private void validateContractOwner(
      Contract contract,
      OrganizationAccount organization) {
    if (!(contract.getOwner() instanceof OrganizationAccount contractOwner)
        || !Objects.equals(contractOwner.getId(), organization.getId())) {
      throw new SecurityException("The contract is not owned by this organization");
    }
  }
}
