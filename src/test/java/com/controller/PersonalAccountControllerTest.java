package com.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.App;

@SpringBootTest(classes = App.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonalAccountControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void clearAccounts() {
    mongoTemplate.dropCollection("accounts");
  }

  @Test
  void registersAndReturnsTokenForProtectedProfile() throws Exception {
    MvcResult registration = mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"email":"octocat@github.com","password":"secure-password"}
            """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("octocat@github.com"))
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andReturn();

    String token = tokenFrom(registration);
    mockMvc.perform(get("/api/accounts/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("octocat@github.com"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void rejectsUnauthenticatedProfileRequest() throws Exception {
    mockMvc.perform(get("/api/accounts/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logsInUpdatesAndDeletesOwnAccount() throws Exception {
    registerAccount();
    MvcResult login = mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"email":"octocat@github.com","password":"secure-password"}
            """))
        .andExpect(status().isOk())
        .andReturn();
    String token = tokenFrom(login);
    String authorization = "Bearer " + token;

    mockMvc.perform(patch("/api/accounts/me")
        .header("Authorization", authorization)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"email":"hubot@github.com","password":"updated-password"}
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("hubot@github.com"));

    mockMvc.perform(delete("/api/accounts/me").header("Authorization", authorization))
        .andExpect(status().isNoContent());
    mockMvc.perform(get("/api/accounts/me").header("Authorization", authorization))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsDuplicateRegistrationAndInvalidLogin() throws Exception {
    registerAccount();
    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"email":"octocat@github.com","password":"another-password"}
            """))
        .andExpect(status().isConflict());
    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"email":"octocat@github.com","password":"wrong-password"}
            """))
        .andExpect(status().isUnauthorized());
  }

  private void registerAccount() throws Exception {
    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"email":"octocat@github.com","password":"secure-password"}
            """))
        .andExpect(status().isCreated());
  }

  private String tokenFrom(MvcResult result) throws Exception {
    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
    return response.get("token").asText();
  }
}
