package com.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Document(collection = "accounts")
@TypeAlias("personal_account")
public class PersonalAccount extends Account {
  private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

  @Indexed(unique = true)
  private String email;
  private String passwordHash;

  public PersonalAccount() {
  }

  public PersonalAccount(String email, String password) {
    this.email = email;
    setPassword(password);
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @JsonIgnore
  public String getPasswordHash() {
    return passwordHash;
  }

  @JsonIgnore
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public void setPassword(String password) {
    if (password == null || password.isBlank()) {
      throw new IllegalArgumentException("Password must not be blank");
    }
    this.passwordHash = PASSWORD_ENCODER.encode(password);
  }

  public boolean matchesPassword(String password) {
    return password != null
        && passwordHash != null
        && PASSWORD_ENCODER.matches(password, passwordHash);
  }

}
