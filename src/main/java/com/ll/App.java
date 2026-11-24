package com.ll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.repository.AccountRepository;

@SpringBootApplication(
    scanBasePackages = "com",
    exclude = UserDetailsServiceAutoConfiguration.class)
@EnableMongoRepositories(basePackageClasses = AccountRepository.class)
public class App {
  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }
}
