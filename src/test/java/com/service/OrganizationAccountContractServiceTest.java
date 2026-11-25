package com.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exceptions.ContractNotFoundException;
import com.exceptions.OrganizationNotFoundException;
import com.model.Contract;
import com.model.OrganizationAccount;
import com.model.OrganizationMembership;
import com.model.OrganizationRole;
import com.model.PersonalAccount;
import com.repository.AccountRepository;
import com.repository.ContractRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationAccountContractServiceTest {
  @Mock
  private ContractRepository contractRepository;

  @Mock
  private AccountRepository accountRepository;

  private OrganizationContractService organizationContractService;

  @BeforeEach
  void setUp() {
    organizationContractService =
        new OrganizationContractService(contractRepository, accountRepository);
  }

  @Test
  void ownerCanCreateOrganizationContract() {
    PersonalAccount owner = account("owner-1");
    OrganizationAccount organization = organization(
        "owner-1", OrganizationRole.owner);
    when(accountRepository.findById("owner-1")).thenReturn(Optional.of(owner));
    when(accountRepository.findOrganizationById("organization-1"))
        .thenReturn(Optional.of(organization));
    when(contractRepository.save(any(Contract.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Contract contract = organizationContractService.createContract(
        owner, "organization-1", "GitHub Enterprise", "Annual plan");

    assertEquals("GitHub Enterprise", contract.getName());
    assertEquals("Annual plan", contract.getDescription());
    assertSame(organization, contract.getOwner());
    verify(contractRepository).save(contract);
  }

  @Test
  void editorCanCreateOrganizationContract() {
    PersonalAccount editor = account("editor-1");
    OrganizationAccount organization = organization(
        "owner-1", OrganizationRole.viewer);
    organization.addMember("editor-1", OrganizationRole.editor);
    when(accountRepository.findById("editor-1")).thenReturn(Optional.of(editor));
    when(accountRepository.findOrganizationById("organization-1"))
        .thenReturn(Optional.of(organization));
    when(contractRepository.save(any(Contract.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Contract contract = organizationContractService.createContract(
        editor, "organization-1", "Team plan", "Monthly plan");

    assertSame(organization, contract.getOwner());
  }

  @Test
  void viewerCannotCreateOrganizationContract() {
    PersonalAccount viewer = account("viewer-1");
    OrganizationAccount organization = organization(
        "owner-1", OrganizationRole.owner);
    organization.addMember("viewer-1", OrganizationRole.viewer);
    when(accountRepository.findById("viewer-1")).thenReturn(Optional.of(viewer));
    when(accountRepository.findOrganizationById("organization-1"))
        .thenReturn(Optional.of(organization));

    assertThrows(SecurityException.class,
        () -> organizationContractService.createContract(
            viewer, "organization-1", "Viewer plan", "Not allowed"));

    verify(contractRepository, never()).save(any(Contract.class));
  }

  @Test
  void memberCanGetOrganizationContract() {
    PersonalAccount viewer = account("viewer-1");
    OrganizationAccount organization = organization(
        "owner-1", OrganizationRole.owner);
    organization.addMember("viewer-1", OrganizationRole.viewer);
    Contract contract = new Contract(
        "GitHub Enterprise", "Annual plan", organization);
    when(accountRepository.findById("viewer-1")).thenReturn(Optional.of(viewer));
    when(accountRepository.findOrganizationById("organization-1"))
        .thenReturn(Optional.of(organization));
    when(contractRepository.findById(contract.getId()))
        .thenReturn(Optional.of(contract));

    Contract result = organizationContractService.getContract(
        viewer, "organization-1", contract.getId());

    assertSame(contract, result);
  }

  @Test
  void nonMemberCannotGetOrganizationContract() {
    PersonalAccount outsider = account("outsider-1");
    OrganizationAccount organization = organization(
        "owner-1", OrganizationRole.owner);
    Contract contract = new Contract(
        "GitHub Enterprise", "Annual plan", organization);
    when(accountRepository.findById("outsider-1")).thenReturn(Optional.of(outsider));
    when(accountRepository.findOrganizationById("organization-1"))
        .thenReturn(Optional.of(organization));

    assertThrows(SecurityException.class,
        () -> organizationContractService.getContract(
            outsider, "organization-1", contract.getId()));

    verify(contractRepository, never()).findById(any(String.class));
  }

  @Test
  void editorCanUpdateAndOwnerCanDeleteOrganizationContract() {
    PersonalAccount editor = account("editor-1");
    PersonalAccount owner = account("owner-1");
    OrganizationAccount organization = organization(
        "owner-1", OrganizationRole.owner);
    organization.addMember("editor-1", OrganizationRole.editor);
    Contract contract = new Contract(
        "Old plan", "Old description", organization);
    when(accountRepository.findById("editor-1")).thenReturn(Optional.of(editor));
    when(accountRepository.findById("owner-1")).thenReturn(Optional.of(owner));
    when(accountRepository.findOrganizationById("organization-1"))
        .thenReturn(Optional.of(organization));
    when(contractRepository.findById(contract.getId()))
        .thenReturn(Optional.of(contract));
    when(contractRepository.save(contract)).thenReturn(contract);

    Contract updated = organizationContractService.updateContract(
        editor, "organization-1", contract.getId(),
        "New plan", "New description");

    assertEquals("New plan", updated.getName());
    assertEquals("New description", updated.getDescription());
    verify(contractRepository).save(contract);

    organizationContractService.deleteContract(
        owner, "organization-1", contract.getId());
    verify(contractRepository).delete(contract);
  }

  @Test
  void viewerCannotUpdateOrDeleteOrganizationContract() {
    PersonalAccount viewer = account("viewer-1");
    OrganizationAccount organization = organization(
        "owner-1", OrganizationRole.owner);
    organization.addMember("viewer-1", OrganizationRole.viewer);
    Contract contract = new Contract(
        "Team plan", "Monthly plan", organization);
    when(accountRepository.findById("viewer-1"))
        .thenReturn(Optional.of(viewer));
    when(accountRepository.findOrganizationById("organization-1"))
        .thenReturn(Optional.of(organization));

    assertThrows(SecurityException.class, () ->
        organizationContractService.updateContract(
            viewer, "organization-1", contract.getId(), "Changed", "Changed"));
    assertThrows(SecurityException.class, () ->
        organizationContractService.deleteContract(
            viewer, "organization-1", contract.getId()));

    verify(contractRepository, never()).save(any(Contract.class));
    verify(contractRepository, never()).delete(any(Contract.class));
  }

  @Test
  void rejectsMissingOrganizationAndContract() {
    PersonalAccount owner = account("owner-1");
    when(accountRepository.findById("owner-1")).thenReturn(Optional.of(owner));
    when(accountRepository.findOrganizationById("missing"))
        .thenReturn(Optional.empty());
    when(contractRepository.findById("missing-contract"))
        .thenReturn(Optional.empty());

    assertThrows(OrganizationNotFoundException.class,
        () -> organizationContractService.createContract(
            owner, "missing", "Plan", "Description"));
    assertThrows(ContractNotFoundException.class,
        () -> organizationContractService.getContract(
            owner, "missing-contract"));
  }

  private PersonalAccount account(String id) {
    PersonalAccount account =
        new PersonalAccount(id + "@example.com", "password");
    account.setId(id);
    return account;
  }

  private OrganizationAccount organization(
      String ownerId, OrganizationRole ownerRole) {
    OrganizationAccount organization = new OrganizationAccount(
        ownerId,
        List.of(new OrganizationMembership(ownerId, ownerRole)),
        "Tech Corp",
        "Tech Corp organization");
    organization.setId("organization-1");
    return organization;
  }
}
