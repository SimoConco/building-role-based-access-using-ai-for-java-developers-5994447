package com.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PersonalAccountTest {
  @Test
  void hashesPasswordAndVerifiesIt() {
    PersonalAccount account = new PersonalAccount("user@example.com", "correct-horse-battery");

    assertNotEquals("correct-horse-battery", account.getPasswordHash());
    assertTrue(account.matchesPassword("correct-horse-battery"));
    assertFalse(account.matchesPassword("wrong-password"));
  }
}
