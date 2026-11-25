package com.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.exceptions.AccountNotFoundException;
import com.exceptions.OrganizationNotFoundException;
import com.model.OrganizationAccount;
import com.model.OrganizationRole;
import com.model.PersonalAccount;
import com.repository.AccountRepository;

@Service
public class OrganizationAccountService {
  private final AccountRepository accountRepository;
  private final PersonalAccountService personalAccountService;

  public OrganizationAccountService(
      AccountRepository accountRepository, PersonalAccountService personalAccountService) {
    this.accountRepository = accountRepository;
    this.personalAccountService = personalAccountService;
  }

  public OrganizationAccount createOrganization(
      PersonalAccount personalAccount, String name, String description) {
    if (personalAccount == null || personalAccount.getId() == null || personalAccount.getId().isBlank()) {
      throw new AccountNotFoundException(personalAccount == null ? null : personalAccount.getId());
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Organization name must not be blank");
    }

    PersonalAccount owner = personalAccountService.getPersonalAccount(personalAccount, personalAccount.getId());
    OrganizationAccount organization = new OrganizationAccount(owner.getId(), null, name, description);
    organization.addMember(owner.getId(), OrganizationRole.owner);
    return accountRepository.saveOrganization(organization);
  }

  public OrganizationAccount getOrganization(PersonalAccount requestingAccount, String organizationId) {
    PersonalAccount member = requireAccount(requestingAccount);
    OrganizationAccount organization = findOrganization(organizationId);
    if (!organization.isMember(member.getId())) {
      throw new SecurityException("Only organization members can access the organization");
    }

    return organization;
  }

  public OrganizationAccount addMember(
      PersonalAccount requestingAccount, String organizationId, String memberId, OrganizationRole role) {
    OrganizationAccount organization = getOrganizationForOwner(requestingAccount, organizationId);
    requireAccountById(memberId);
    organization.addMember(memberId, role);
    return accountRepository.saveOrganization(organization);
  }

  public OrganizationAccount updateMemberRole(
      PersonalAccount requestingAccount, String organizationId, String memberId, OrganizationRole role) {
    OrganizationAccount organization = getOrganizationForOwner(requestingAccount, organizationId);
    organization.updateMemberRole(memberId, role);
    return accountRepository.saveOrganization(organization);
  }

  public OrganizationAccount removeMember(
      PersonalAccount requestingAccount, String organizationId, String memberId) {
    OrganizationAccount organization = getOrganizationForOwner(requestingAccount, organizationId);
    if (Objects.equals(organization.getOwnerId(), memberId)) {
      throw new IllegalArgumentException("Organization owner cannot remove themselves");
    }

    organization.removeMember(memberId);
    return accountRepository.saveOrganization(organization);
  }

  public void deleteOrganization(PersonalAccount requestingAccount, String organizationId) {
    OrganizationAccount organization = getOrganizationForOwner(requestingAccount, organizationId);
    accountRepository.deleteOrganization(organization);
  }

  private OrganizationAccount getOrganizationForOwner(
      PersonalAccount requestingAccount, String organizationId) {
    PersonalAccount owner = requireAccount(requestingAccount);
    OrganizationAccount organization = findOrganization(organizationId);
    if (!owner.getId().equals(organization.getOwnerId())) {
      throw new SecurityException("Only the organization owner can manage organization members");
    }

    return organization;
  }

  private OrganizationAccount findOrganization(String organizationId) {
    if (organizationId == null || organizationId.isBlank()) {
      throw new IllegalArgumentException("Organization ID must not be blank");
    }
    return accountRepository.findOrganizationById(organizationId)
        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
  }

  private PersonalAccount requireAccount(PersonalAccount requestingAccount) {
    if (requestingAccount == null || requestingAccount.getId() == null
        || requestingAccount.getId().isBlank()) {
      throw new AccountNotFoundException(requestingAccount == null ? null : requestingAccount.getId());
    }
    return personalAccountService.getPersonalAccount(requestingAccount, requestingAccount.getId());
  }

  private PersonalAccount requireAccountById(String accountId) {
    if (accountId == null || accountId.isBlank()) {
      throw new IllegalArgumentException("Member ID must not be blank");
    }
    return accountRepository.findById(accountId)
        .orElseThrow(() -> new AccountNotFoundException(accountId));
  }
}
