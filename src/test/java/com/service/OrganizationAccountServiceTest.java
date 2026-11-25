package com.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exceptions.AccountNotFoundException;
import com.exceptions.OrganizationNotFoundException;
import com.model.OrganizationAccount;
import com.model.OrganizationMembership;
import com.model.OrganizationRole;
import com.model.PersonalAccount;
import com.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationAccountServiceTest {
  @Mock
  private AccountRepository accountRepository;

  @Test
  void createsOrganizationOwnedByPersonalAccount() {
    PersonalAccount owner = new PersonalAccount("owner@example.com", "password");
    owner.setId("owner-1");
    when(accountRepository.findById("owner-1")).thenReturn(Optional.of(owner));
    when(accountRepository.saveOrganization(any(OrganizationAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    OrganizationAccountService organizationAccountService =
        new OrganizationAccountService(accountRepository, new PersonalAccountService(accountRepository));

    OrganizationAccount organization =
        organizationAccountService.createOrganization(owner, "Acme", "Acme organization");

    assertEquals("owner-1", organization.getOwnerId());
    assertEquals("Acme", organization.getName());
    assertEquals("Acme organization", organization.getDescription());
    assertEquals(OrganizationRole.owner, organization.getMemberRole("owner-1"));
    verify(accountRepository).findById("owner-1");
    verify(accountRepository).saveOrganization(argThat(saved ->
        Objects.equals("Acme", saved.getName()) && Objects.equals("owner-1", saved.getOwnerId())));
  }

  @Test
  void rejectsMissingOwnerAndBlankName() {
    OrganizationAccountService organizationAccountService =
        new OrganizationAccountService(accountRepository, new PersonalAccountService(accountRepository));
    assertThrows(AccountNotFoundException.class,
        () -> organizationAccountService.createOrganization(null, "Acme", null));

    PersonalAccount owner = new PersonalAccount("owner@example.com", "password");
    owner.setId("owner-1");
    assertThrows(IllegalArgumentException.class,
        () -> organizationAccountService.createOrganization(owner, " ", null));
  }

  @Test
  void anyMemberCanGetOrganization() {
    PersonalAccount member = account("member-1");
    OrganizationAccount organization = organization();
    organization.addMember(member.getId(), OrganizationRole.viewer);
    when(accountRepository.findById(member.getId())).thenReturn(Optional.of(member));
    when(accountRepository.findOrganizationById("organization-1")).thenReturn(Optional.of(organization));
    OrganizationAccountService service = service();

    assertSame(organization, service.getOrganization(member, "organization-1"));
  }

  @Test
  void nonOwnerCannotManageMembersOrDeleteOrganization() {
    PersonalAccount member = account("member-1");
    OrganizationAccount organization = organization();
    organization.addMember(member.getId(), OrganizationRole.editor);
    when(accountRepository.findById(member.getId())).thenReturn(Optional.of(member));
    when(accountRepository.findOrganizationById("organization-1")).thenReturn(Optional.of(organization));
    OrganizationAccountService service = service();

    assertThrows(SecurityException.class,
        () -> service.addMember(member, "organization-1", "another-member", OrganizationRole.viewer));
    assertThrows(SecurityException.class,
        () -> service.updateMemberRole(member, "organization-1", "member-1", OrganizationRole.viewer));
    assertThrows(SecurityException.class,
        () -> service.removeMember(member, "organization-1", "member-1"));
    assertThrows(SecurityException.class, () -> service.deleteOrganization(member, "organization-1"));

    verify(accountRepository, never()).saveOrganization(any(OrganizationAccount.class));
    verify(accountRepository, never()).deleteOrganization(any(OrganizationAccount.class));
  }

  @Test
  void ownerCanManageMembersAndDeleteOrganization() {
    PersonalAccount owner = account("owner-1");
    PersonalAccount member = account("member-1");
    OrganizationAccount organization = organization();
    organization.addMember(member.getId(), OrganizationRole.viewer);
    when(accountRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
    when(accountRepository.findById("member-2")).thenReturn(Optional.of(account("member-2")));
    when(accountRepository.findOrganizationById("organization-1")).thenReturn(Optional.of(organization));
    when(accountRepository.saveOrganization(any(OrganizationAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    OrganizationAccountService service = service();

    service.addMember(owner, "organization-1", "member-2", OrganizationRole.viewer);
    service.updateMemberRole(owner, "organization-1", member.getId(), OrganizationRole.editor);
    service.removeMember(owner, "organization-1", "member-2");

    assertEquals(OrganizationRole.editor, organization.getMemberRole(member.getId()));
    assertFalse(organization.isMember("member-2"));
    verify(accountRepository, org.mockito.Mockito.times(3)).saveOrganization(organization);

    service.deleteOrganization(owner, "organization-1");
    verify(accountRepository).deleteOrganization(organization);
  }

  @Test
  void ownerCannotRemoveThemselves() {
    PersonalAccount owner = account("owner-1");
    OrganizationAccount organization = organization();
    when(accountRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
    when(accountRepository.findOrganizationById("organization-1")).thenReturn(Optional.of(organization));
    OrganizationAccountService service = service();

    assertThrows(IllegalArgumentException.class,
        () -> service.removeMember(owner, "organization-1", owner.getId()));
    verify(accountRepository, never()).saveOrganization(any(OrganizationAccount.class));
  }

  @Test
  void rejectsNonMembersAndMissingOrganizations() {
    PersonalAccount account = account("outsider");
    OrganizationAccount organization = organization();
    when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
    when(accountRepository.findOrganizationById("organization-1")).thenReturn(Optional.of(organization));
    OrganizationAccountService service = service();

    assertThrows(SecurityException.class, () -> service.getOrganization(account, "organization-1"));
    assertThrows(OrganizationNotFoundException.class, () -> service.getOrganization(account, "missing"));
  }

  private OrganizationAccountService service() {
    return new OrganizationAccountService(accountRepository, new PersonalAccountService(accountRepository));
  }

  private PersonalAccount account(String id) {
    PersonalAccount account = new PersonalAccount(id + "@example.com", "password");
    account.setId(id);
    return account;
  }

  private OrganizationAccount organization() {
    OrganizationAccount organization = new OrganizationAccount(
        "owner-1", List.of(new OrganizationMembership("owner-1", OrganizationRole.owner)),
        "Acme", "Acme organization");
    organization.setId("organization-1");
    return organization;
  }


  /**
   * 1. Create a Personal Account for Alice
   * 2. Create an Organization Account for Tech Corp with Alice as the owner
   * 3. verify that Alice can manage the organization and its members
   * 4. Validate that the organization was created successfully and can be retrieved by owner ID
   * 5. Delete the organization and verify that it is no longer accessible
   */

  @Test 
  void testOwnerOrganizationManagementFlow(){
    // Step 1: Create a Personal Account for Alice
    PersonalAccount aliceAccount = new PersonalAccount("alice@example.com", "password");
    aliceAccount.setId("alice-1");
    when(accountRepository.findById("alice-1")).thenReturn(Optional.of(aliceAccount));
    when(accountRepository.findById("member-1")).thenReturn(Optional.of(account("member-1")));

    // Step 2: Create an Organization Account for Tech Corp with Alice as the owner
    OrganizationAccount techCorpOrganization = new OrganizationAccount(
        "alice-1", List.of(new OrganizationMembership("alice-1", OrganizationRole.owner)),
        "Tech Corp", "Tech Corp organization");
    techCorpOrganization.setId("tech-corp-1");
    when(accountRepository.findOrganizationById("tech-corp-1")).thenReturn(Optional.of(techCorpOrganization));
    when(accountRepository.saveOrganization(any(OrganizationAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));   
        
    OrganizationAccountService organizationAccountService = service();

    // Step 3: Verify that Alice can manage the organization and its members
    organizationAccountService.addMember(aliceAccount, "tech-corp-1", "member-1", OrganizationRole.viewer);
    organizationAccountService.updateMemberRole(aliceAccount, "tech-corp-1", "member-1", OrganizationRole.editor);
    organizationAccountService.removeMember(aliceAccount, "tech-corp-1", "member-1"); 

    // Step 4: Validate that the organization was created successfully and can be retrieved by owner ID
    OrganizationAccount retrievedOrganization = organizationAccountService.getOrganization(aliceAccount, "tech-corp-1");
    assertEquals("Tech Corp", retrievedOrganization.getName());

    // Step 5: Delete the organization and verify that it is no longer accessible
    organizationAccountService.deleteOrganization(aliceAccount, "tech-corp-1");
    when(accountRepository.findOrganizationById("tech-corp-1")).thenReturn(Optional.empty());
    assertThrows(OrganizationNotFoundException.class, () -> organizationAccountService.getOrganization(aliceAccount, "tech-corp-1"));

  }

  /**
   * 1. Create personal accounts for Alice, Bob, and Charlie
   * 2. Create an organization account for Tech Corp with Alice as the owner
   * 3. Add Bob and Charlie as members of the organization with different roles
   * 4. Verify that Bob and Charlie can access the organization and their roles are correctly assigned
   * 5. Attempt to have Bob remove Charlie from the organization and verify that it fails due to insufficient permissions
   * 6. Have Alice remove Charlie from the organization and verify that it succeeds
   * 7. Attempt to have Charlie access the organization after being removed and verify that it fails
   * 8. Attempt to have Alice remove herself from the organization and verify that it fails
   * 9. Delete the organization and verify that it is no longer accessible by any member
   * 10. Attempt to have Bob access the organization after it has been deleted and verify that it fails
   */

  @Test 
  void testMemberAccessAndRoleManagementFlow() {
    // Step 1: Create personal accounts for Alice, Bob, and Charlie
    PersonalAccount aliceAccount = new PersonalAccount("Alice", "password");
    PersonalAccount bobAccount = new PersonalAccount("Bob", "password");
    PersonalAccount charlieAccount = new PersonalAccount("Charlie", "password");

    aliceAccount.setId("alice-1");
    bobAccount.setId("bob-1");
    charlieAccount.setId("charlie-1");

    when(accountRepository.findById("alice-1")).thenReturn(Optional.of(aliceAccount));
    when(accountRepository.findById("bob-1")).thenReturn(Optional.of(bobAccount));
    when(accountRepository.findById("charlie-1")).thenReturn(Optional.of(charlieAccount));

    // Step 2: Create an organization account for Tech Corp with Alice as the owner
    OrganizationAccount techCorpOrganization = new OrganizationAccount(
        "alice-1", List.of(new OrganizationMembership("alice-1", OrganizationRole.owner)),
        "Tech Corp", "Tech Corp organization");
    techCorpOrganization.setId("tech-corp-1");
    when(accountRepository.findOrganizationById("tech-corp-1")).thenReturn(Optional.of(techCorpOrganization));
    when(accountRepository.saveOrganization(any(OrganizationAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    OrganizationAccountService organizationAccountService = service();

    // Step 3: Add Bob and Charlie as members of the organization with different roles
    organizationAccountService.addMember(aliceAccount, "tech-corp-1", "bob-1", OrganizationRole.viewer);
    organizationAccountService.addMember(aliceAccount, "tech-corp-1", "charlie-1", OrganizationRole.editor);

    // Step 4: Verify that Bob and Charlie can access the organization and their roles are correctly assigned
    OrganizationAccount bobOrganization = organizationAccountService.getOrganization(bobAccount, "tech-corp-1");
    OrganizationAccount charlieOrganization = organizationAccountService.getOrganization(charlieAccount, "tech-corp-1");
    assertEquals(OrganizationRole.viewer, bobOrganization.getMemberRole("bob-1"));
    assertEquals(OrganizationRole.editor, charlieOrganization.getMemberRole("charlie-1"));  

    // Step 5: Attempt to have Bob remove Charlie from the organization and verify that it fails due to insufficient permissions
    assertThrows(SecurityException.class, () -> organizationAccountService.removeMember(bobAccount, "tech-corp-1", "charlie-1"));

    // Step 6: Have Alice remove Charlie from the organization and verify that it succeeds
    organizationAccountService.removeMember(aliceAccount, "tech-corp-1", "charlie-1");
    assertFalse(techCorpOrganization.isMember("charlie-1"));

    // Step 7: Attempt to have Charlie access the organization after being removed and verify that it fails
    assertThrows(SecurityException.class, () -> organizationAccountService.getOrganization(charlieAccount, "tech-corp-1"));

    // Step 8: Attempt to have Alice remove herself from the organization and verify that it fails
    assertThrows(IllegalArgumentException.class, () -> organizationAccountService.removeMember(aliceAccount, "tech-corp-1", "alice-1"));

    // Step 9: Delete the organization and verify that it is no longer accessible by any member
    organizationAccountService.deleteOrganization(aliceAccount, "tech-corp-1");
    when(accountRepository.findOrganizationById("tech-corp-1")).thenReturn(Optional.empty());
    assertThrows(OrganizationNotFoundException.class, () -> organizationAccountService.getOrganization(aliceAccount, "tech-corp-1"));
    assertThrows(OrganizationNotFoundException.class, () -> organizationAccountService.getOrganization(bobAccount, "tech-corp-1"));

    // Step 10: Attempt to have Bob access the organization after it has been deleted and verify that it fails
    assertThrows(OrganizationNotFoundException.class, () -> organizationAccountService.getOrganization(bobAccount, "tech-corp-1"));

  }
}
