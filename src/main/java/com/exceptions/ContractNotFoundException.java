package com.exceptions;

public class ContractNotFoundException extends RuntimeException {
  public ContractNotFoundException(String contractId) {
    super("Contract not found: " + contractId);
  }
}
