package com.repository;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.model.OrganizationAccount;

@Repository
public class OrganizationAccountRepositoryImpl implements OrganizationAccountRepository {
  private final MongoTemplate mongoTemplate;

  public OrganizationAccountRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public OrganizationAccount saveOrganization(OrganizationAccount organizationAccount) {
    return mongoTemplate.save(organizationAccount);
  }

  @Override
  public void deleteOrganization(OrganizationAccount organizationAccount) {
    mongoTemplate.remove(organizationAccount);
  }
}
