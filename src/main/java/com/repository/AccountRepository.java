package com.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.model.PersonalAccount;

public interface AccountRepository extends MongoRepository<PersonalAccount, String> {
  Optional<PersonalAccount> findByEmail(String email);

  boolean existsByEmail(String email);

  void deletePersonalAccountByEmail(String email);
}
