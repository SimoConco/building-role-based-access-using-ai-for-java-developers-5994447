package com.model;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "accounts")
@TypeAlias("organization_account")
public class OrganizationAccount extends Account {
  public OrganizationAccount() {
  }
}
