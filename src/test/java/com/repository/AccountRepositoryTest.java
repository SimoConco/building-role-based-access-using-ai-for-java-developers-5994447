package com.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.model.OrganizationAccount;
import com.model.OrganizationRole;
import com.model.PersonalAccount;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@SpringJUnitConfig(AccountRepositoryTest.MongoConfiguration.class)
@TestPropertySource("classpath:application-test.properties")
public class AccountRepositoryTest {
  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private MongoTemplate mongoTemplate;

  @BeforeEach
  void clearAccounts() {
    mongoTemplate.dropCollection(PersonalAccount.class);
  }

  @Test
  void testExistsByEmail() {
    String email = "octocat@github.com";
    accountRepository.save(new PersonalAccount(email, "secure-password"));

    assertTrue(accountRepository.existsByEmail(email));
    assertFalse(accountRepository.existsByEmail("missing@github.com"));
  }

  @Test
  void testFindByEmail() {
    String email = "hubot@github.com";
    PersonalAccount account = accountRepository.save(new PersonalAccount(email, "secure-password"));

    Optional<PersonalAccount> result = accountRepository.findByEmail(email);

    assertTrue(result.isPresent());
    assertEquals(account.getId(), result.get().getId());
    assertEquals(email, result.get().getEmail());
    assertTrue(result.get().matchesPassword("secure-password"));
    assertTrue(accountRepository.findByEmail("missing@github.com").isEmpty());
  }

  @Test
  void testSaveNewAccount() {
    PersonalAccount account = accountRepository.save(
        new PersonalAccount("octocat@github.com", "secure-password"));
    assertNotNull(account.getId());

    PersonalAccount created = accountRepository.findById(account.getId()).orElseThrow();
    assertEquals("octocat@github.com", created.getEmail());
    assertTrue(created.matchesPassword("secure-password"));
  }

  @Test
  void testUpdateAccount() {
    PersonalAccount account = accountRepository.save(
        new PersonalAccount("octocat@github.com", "secure-password"));
    account.setEmail("hubot@github.com");

    accountRepository.save(account);

    PersonalAccount updated = accountRepository.findById(account.getId()).orElseThrow();
    assertEquals("hubot@github.com", updated.getEmail());
    assertFalse(accountRepository.existsByEmail("octocat@github.com"));
  }

  @Test
  void testDeleteAccount() {
    PersonalAccount account = accountRepository.save(
        new PersonalAccount("octocat@github.com", "secure-password"));

    accountRepository.deleteById(account.getId());

    assertFalse(accountRepository.existsById(account.getId()));
    assertTrue(accountRepository.findById(account.getId()).isEmpty());
  }

  @Configuration
  @EnableMongoRepositories(basePackageClasses = AccountRepository.class)
  static class MongoConfiguration {
    @Bean(destroyMethod = "close")
    MongoClient mongoClient(@Value("${spring.data.mongodb.uri}") String connectionString) {
      return MongoClients.create(connectionString);
    }

    @Bean
    MongoDatabaseFactory mongoDatabaseFactory(
        MongoClient mongoClient,
        @Value("${spring.data.mongodb.database}") String databaseName) {
      return new SimpleMongoClientDatabaseFactory(mongoClient, databaseName);
    }

    @Bean
    MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
      return new MongoTemplate(mongoDatabaseFactory);
    }
  }

  /**
   * 1. Create a Personal Account for Alice
   * 2. Validate that the account was created successfully and can be retrieved by email
   * 3. Create an Organization Account for Alice
   * 4. Validate that the account was created successfully and can be retrieved by owner ID
   */

  @Test 
  void testCompleteOrganizationAccountCRUDOperation() {
    // Step 1: Create a Personal Account for Alice
    PersonalAccount personalAccount = new PersonalAccount("alice@github.com", "secure-password");
    personalAccount = accountRepository.save(personalAccount);

    // Step 2: Validate that the account was created successfully and can be retrieved by email
    assertTrue(accountRepository.existsByEmail("alice@github.com"));
    assertEquals(personalAccount.getId(), accountRepository.findByEmail("alice@github.com").get().getId());

    // Step 3: Create an Organization Account for Alice
    OrganizationAccount organizationAccount = new OrganizationAccount("GitHub", null, personalAccount.getId().toString(), null);
    organizationAccount = (OrganizationAccount) mongoTemplate.save(organizationAccount);

    // Step 4: Validate that the account was created successfully and can be retrieved by owner ID
    assertTrue(accountRepository.existsById(organizationAccount.getId()));
    assertEquals(organizationAccount.getId(), accountRepository.findById(organizationAccount.getId()).get().getId());
  }

  /**
   * 1. Create a Personal Account for Alice
   * 2. Create an Organization Account for Alice
   * 3. Add a member to the Organization Account
   * 4. Validate that the member was added successfully and can be retrieved by member ID
   * 5. Update the member's role in the Organization Account
   * 6. Validate that the member's role was updated successfully
   * 7. Remove the member from the Organization Account
   * 8. Validate that the member was removed successfully and can no longer be retrieved by member ID
   */
  @Test 
  void testOrganizationAccountMemberManagement() {
    // Step 1: Create a Personal Account for Alice
    PersonalAccount personalAccount = new PersonalAccount("alice@github.com", "secure-password");
    personalAccount = accountRepository.save(personalAccount);

    // Step 2: Create an Organization Account for Alice
    OrganizationAccount organizationAccount = new OrganizationAccount("GitHub", null, personalAccount.getId().toString(), null);
    organizationAccount = (OrganizationAccount) mongoTemplate.save(organizationAccount);

    // Step 3: Add a member to the Organization Account
    organizationAccount.addMember("member-1", OrganizationRole.viewer); 

    // Step 4: Validate that the member was added successfully and can be retrieved by member ID
    assertTrue(organizationAccount.isMember("member-1"));
    assertEquals(OrganizationRole.viewer, organizationAccount.getMemberRole("member-1"));

    // Step 5: Update the member's role in the Organization Account
    organizationAccount.updateMemberRole("member-1", OrganizationRole.editor);

    // Step 6: Validate that the member's role was updated successfully
    assertEquals(OrganizationRole.editor, organizationAccount.getMemberRole("member-1"));

    // Step 7: Remove the member from the Organization Account
    organizationAccount.removeMember("member-1");

    // Step 8: Validate that the member was removed successfully and can no longer be retrieved by member ID
    assertFalse(organizationAccount.isMember("member-1"));
  }
}