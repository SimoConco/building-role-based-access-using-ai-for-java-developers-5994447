package com.model;

import java.time.Instant;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "contracts")
@TypeAlias("contract")
public class Contract extends Resource<Contract> {
  private String name;
  private String description;

  // Required by Spring Data MongoDB
  protected Contract() {
    super();
  }

  public Contract(String name, String description, Account owner) {
    super(owner);
    this.name = name;
    this.description = description;
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

  public void update(String name, String description) {
    this.name = name;
    this.description = description;
    setUpdatedAt(Instant.now());
  }
}
