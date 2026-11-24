package com.ll;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@SpringJUnitConfig(DbOperationsTest.MongoConfiguration.class)
@TestPropertySource("classpath:application-test.properties")
public class DbOperationsTest {
  @Autowired
  private MongoClient mongoClient;

  @Value("${app.test.mongodb.crud-database}")
  private String databaseName;

  @Value("${app.test.mongodb.crud-collection}")
  private String collectionName;

  private MongoDatabase database;
  private MongoCollection<Document> employees;

  @BeforeEach
  void setUp() {
    database = mongoClient.getDatabase(databaseName);
    database.runCommand(new Document("ping", 1));
    database.drop();
    database.createCollection(collectionName);
    employees = database.getCollection(collectionName);
  }

  @AfterEach
  void tearDown() {
    if (mongoClient != null) {
      mongoClient.getDatabase(databaseName).drop();
    }
  }

  @Test
  void createsEmployee() {
    Employee employee = new Employee("1", "Ada Lovelace", "Engineering", 120000);

    employees.insertOne(employee.toDocument());

    assertEquals(1, employees.countDocuments());
    Document created = employees.find(eq("_id", employee.getId())).first();
    assertNotNull(created);
    assertEquals(employee.getName(), created.getString("name"));
  }

  @Test
  void readsEmployee() {
    Employee employee = new Employee("2", "Grace Hopper", "Research", 125000);
    employees.insertOne(employee.toDocument());

    Employee result = Employee.fromDocument(employees.find(eq("_id", employee.getId())).first());

    assertNotNull(result);
    assertEquals(employee.getId(), result.getId());
    assertEquals(employee.getName(), result.getName());
    assertEquals(employee.getDepartment(), result.getDepartment());
    assertEquals(employee.getSalary(), result.getSalary());
  }

  @Test
  void updatesEmployee() {
    Employee employee = new Employee("3", "Katherine Johnson", "Research", 110000);
    employees.insertOne(employee.toDocument());

    assertEquals(1, employees.updateOne(eq("_id", employee.getId()), set("department", "Aerospace"))
        .getModifiedCount());
    Employee updated = Employee.fromDocument(employees.find(eq("_id", employee.getId())).first());

    assertNotNull(updated);
    assertEquals("Aerospace", updated.getDepartment());
  }

  @Test
  void deletesEmployee() {
    Employee employee = new Employee("4", "Mary Jackson", "Engineering", 105000);
    employees.insertOne(employee.toDocument());

    assertEquals(1, employees.deleteOne(eq("_id", employee.getId())).getDeletedCount());

    assertNull(employees.find(eq("_id", employee.getId())).first());
    assertEquals(0, employees.countDocuments());
  }

  @Configuration
  static class MongoConfiguration {
    @Bean(destroyMethod = "close")
    MongoClient mongoClient(@Value("${spring.data.mongodb.uri}") String connectionString) {
      return MongoClients.create(connectionString);
    }
  }
}

class Employee {
  private final String id;
  private final String name;
  private final String department;
  private final double salary;

  Employee(String id, String name, String department, double salary) {
    this.id = id;
    this.name = name;
    this.department = department;
    this.salary = salary;
  }

  String getId() {
    return id;
  }

  String getName() {
    return name;
  }

  String getDepartment() {
    return department;
  }

  double getSalary() {
    return salary;
  }

  Document toDocument() {
    return new Document("_id", id)
        .append("name", name)
        .append("department", department)
        .append("salary", salary);
  }

  static Employee fromDocument(Document document) {
    if (document == null) {
      return null;
    }
    return new Employee(
        document.getString("_id"),
        document.getString("name"),
        document.getString("department"),
        document.getDouble("salary"));
  }
}
