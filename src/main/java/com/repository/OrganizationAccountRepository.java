package com.repository;

import com.model.OrganizationAccount;

public interface OrganizationAccountRepository {
  OrganizationAccount saveOrganization(OrganizationAccount organizationAccount);

  void deleteOrganization(OrganizationAccount organizationAccount);
}
