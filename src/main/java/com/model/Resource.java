package com.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document 
public abstract class Resource<T extends Resource<T>> {
  @Id
  private String id;

  @DBRef
  private Account owner;

  private Instant createdAt;
  private Instant updatedAt;

  // Required by Spring Data MongoDB
  protected Resource() {
  }

  public Resource(Account owner) {
    this();
    this.id = UUID.randomUUID().toString();
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
    this.owner = owner;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Account getOwner() {
    return owner;
  }

  public void setOwner(Account owner) {
    this.owner = owner;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
