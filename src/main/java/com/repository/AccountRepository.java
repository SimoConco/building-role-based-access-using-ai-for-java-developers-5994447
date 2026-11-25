package com.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.DeleteQuery;

import com.model.OrganizationAccount;
import com.model.PersonalAccount;

/**
 * create @Query methods for the following operations:
 * 1. findByEmail(String email): This method retrieves a personal account based on the provided email. It returns an Optional containing the account if found, or an empty Optional if not found.
 * 2. existsByEmail(String email): This method checks if a personal account with the specified email exists in the database. It returns true if the account exists, and false otherwise.
 * 3. deletePersonalAccountByEmail(String email): This method deletes a personal account based on the provided email. It removes the account from the database if it exists.
 * 1. findOrganizationByOwnerId(String ownerId): This method retrieves an organization based on the owner's ID. It returns an Optional containing the organization if found, or an empty Optional if not found.
 * 2. findOrganizationById(String orgId): This method retrieves an organization based on its ID. It returns an Optional containing the organization if found, or an empty Optional if not found.
 * 3. saveOrganization(OrganizationAccount account): This method saves an organization to the database. It persists the organization object to the collection.
 */

public interface AccountRepository extends MongoRepository<PersonalAccount, String>, OrganizationAccountRepository {
  @Query("{ 'email': ?0 }")
  Optional<PersonalAccount> findByEmail(String email);

  boolean existsByEmail(String email);

  @DeleteQuery("{ 'email': ?0 }")
  void deletePersonalAccountByEmail(String email);

  @Query("{ 'ownerId': ?0 }")
  Optional<OrganizationAccount> findOrganizationByOwnerId(String ownerId);

  @Query("{ '_id': ?0 }")
  Optional<OrganizationAccount> findOrganizationById(String orgId);
}
