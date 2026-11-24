package com.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ContractTest {
  @Test
  void storesContractFieldsAndAccountOwner() {
    Account owner = new PersonalAccount();
    Contract contract = new Contract("Service agreement", "Annual service terms", owner);

    assertEquals("Service agreement", contract.getName());
    assertEquals("Annual service terms", contract.getDescription());
    assertSame(owner, contract.getOwner());
  }

  @Test
  void updatesContractDetailsAndTimestamp() {
    Contract contract = new Contract("Old name", "Old description", new PersonalAccount());
    Instant originalCreatedAt = Instant.parse("2020-01-01T00:00:00Z");
    Instant originalUpdatedAt = Instant.parse("2020-01-01T00:00:00Z");
    contract.setCreatedAt(originalCreatedAt);
    contract.setUpdatedAt(originalUpdatedAt);

    contract.update("New name", "New description");

    assertEquals("New name", contract.getName());
    assertEquals("New description", contract.getDescription());
    assertEquals(originalCreatedAt, contract.getCreatedAt());
    assertTrue(contract.getUpdatedAt().isAfter(originalUpdatedAt));
  }
}
