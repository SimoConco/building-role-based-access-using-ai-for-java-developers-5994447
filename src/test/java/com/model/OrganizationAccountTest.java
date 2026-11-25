package com.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class OrganizationAccountTest {
  @Test
  void managesMembersAndRoles() {
    OrganizationAccount organization = new OrganizationAccount();

    organization.addMember("member-1", OrganizationRole.viewer);

    assertTrue(organization.isMember("member-1"));
    assertEquals(OrganizationRole.viewer, organization.getMemberRole("member-1"));
    assertEquals(1, organization.getMembers().size());

    organization.updateMemberRole("member-1", OrganizationRole.editor);
    assertEquals(OrganizationRole.editor, organization.getMemberRole("member-1"));

    organization.removeMember("member-1");
    assertFalse(organization.isMember("member-1"));
    assertTrue(organization.getMembers().isEmpty());
  }

  @Test
  void rejectsInvalidMemberOperations() {
    OrganizationAccount organization = new OrganizationAccount();

    assertThrows(IllegalArgumentException.class, () -> organization.addMember(" ", OrganizationRole.viewer));
    assertThrows(IllegalArgumentException.class, () -> organization.addMember("member-1", null));
    organization.addMember("member-1", OrganizationRole.viewer);

    assertThrows(IllegalArgumentException.class, () -> organization.addMember("member-1", OrganizationRole.editor));
    assertThrows(IllegalArgumentException.class, () -> organization.removeMember("missing"));
    assertThrows(IllegalArgumentException.class,
        () -> organization.updateMemberRole("missing", OrganizationRole.editor));
    assertThrows(IllegalArgumentException.class, () -> organization.getMemberRole("missing"));
    assertThrows(IllegalArgumentException.class, () -> organization.isMember(null));
  }

  @Test
  void returnsAnUnmodifiableMembersList() {
    OrganizationAccount organization = new OrganizationAccount();
    organization.addMember("member-1", OrganizationRole.viewer);

    assertThrows(UnsupportedOperationException.class,
        () -> organization.getMembers().add(new OrganizationMembership("member-2", OrganizationRole.editor)));

    List<OrganizationMembership> input = List.of(new OrganizationMembership("member-3", OrganizationRole.viewer));
    organization.setMembers(input);

    assertThrows(UnsupportedOperationException.class, () -> organization.getMembers().clear());
  }
}
