package com.exceptions;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(EmailAlreadyExistsException.class)
  ResponseEntity<Map<String, String>> handleDuplicateEmail(EmailAlreadyExistsException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", exception.getMessage()));
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", exception.getMessage()));
  }

  @ExceptionHandler(AccountNotFoundException.class)
  ResponseEntity<Map<String, String>> handleAccountNotFound(AccountNotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", exception.getMessage()));
  }

  @ExceptionHandler(ContractNotFoundException.class)
  ResponseEntity<Map<String, String>> handleContractMissing(ContractNotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", exception.getMessage()));
  }

  @ExceptionHandler(SecurityException.class)
  ResponseEntity<Map<String, String>> handleForbidden(SecurityException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(Map.of("error", exception.getMessage()));
  }
}
