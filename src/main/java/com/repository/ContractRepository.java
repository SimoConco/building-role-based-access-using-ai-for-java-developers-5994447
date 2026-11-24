package com.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.model.Contract;

public interface ContractRepository extends MongoRepository<Contract, String> {

}
