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
}
