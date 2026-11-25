package com.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "accounts")
@TypeAlias("organization_account")
public class OrganizationAccount extends Account {
  private String ownerId;
  private List<OrganizationMembership> members = new ArrayList<>();
  private String name;
  private String description;

  public OrganizationAccount() {
  }

  public OrganizationAccount(String ownerId, List<OrganizationMembership> members,
      String name, String description) {
    this.ownerId = ownerId;
    this.members = members == null ? new ArrayList<>() : new ArrayList<>(members);
    this.name = name;
    this.description = description;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public List<OrganizationMembership> getMembers() {
    return Collections.unmodifiableList(members);
  }

  public void setMembers(List<OrganizationMembership> members) {
    this.members = members == null ? new ArrayList<>() : new ArrayList<>(members);
  }

  public void addMember(String memberId, OrganizationRole role) {
    validateMemberId(memberId);
    if (role == null) {
      throw new IllegalArgumentException("Member role must not be null");
    }
    if (isMember(memberId)) {
      throw new IllegalArgumentException("Member is already part of the organization");
    }

    members.add(new OrganizationMembership(memberId, role));
  }

  public void removeMember(String memberId) {
    validateMemberId(memberId);
    OrganizationMembership membership = findMember(memberId);
    if (membership == null) {
      throw new IllegalArgumentException("Member is not part of the organization");
    }

    members.remove(membership);
  }

  public void updateMemberRole(String memberId, OrganizationRole role) {
    validateMemberId(memberId);
    if (role == null) {
      throw new IllegalArgumentException("Member role must not be null");
    }
    OrganizationMembership membership = findMember(memberId);
    if (membership == null) {
      throw new IllegalArgumentException("Member is not part of the organization");
    }

    membership.setRole(role);
  }

  public OrganizationRole getMemberRole(String memberId) {
    validateMemberId(memberId);
    OrganizationMembership membership = findMember(memberId);
    if (membership == null) {
      throw new IllegalArgumentException("Member is not part of the organization");
    }

    return membership.getRole();
  }

  public boolean isMember(String memberId) {
    validateMemberId(memberId);
    return findMember(memberId) != null;
  }

  private OrganizationMembership findMember(String memberId) {
    return members.stream()
        .filter(membership -> membership != null && memberId.equals(membership.getMemberId()))
        .findFirst()
        .orElse(null);
  }

  private void validateMemberId(String memberId) {
    if (memberId == null || memberId.isBlank()) {
      throw new IllegalArgumentException("Member ID must not be blank");
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
