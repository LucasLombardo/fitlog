package com.fitlog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String testEmail = "testuser@example.com";
    private String testPassword = "testpassword";

    @BeforeEach
    void cleanUp() throws Exception {
        // Try to delete the user by email if it exists (ignore errors)
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", testEmail);
        loginUser.put("password", testPassword);
        // Try to login to get the token (if user exists)
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andReturn();
        if (loginResult.getResponse().getStatus() == 200) {
            String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
            // Get all users with the token
            MvcResult usersResult = mockMvc.perform(get("/users")
                    .header("Authorization", "Bearer " + token))
                    .andReturn();
            var users = objectMapper.readTree(usersResult.getResponse().getContentAsString());
            for (var user : users) {
                if (user.get("email").asText().equals(testEmail)) {
                    long id = user.get("id").asLong();
                    mockMvc.perform(delete("/users/" + id)
                            .header("Authorization", "Bearer " + token));
                }
            }
        }
    }

    @Test
    void testUserRegistrationAndLoginAndProtectedEndpoint() throws Exception {
        // 1. Register a new user
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
        createUser.put("role", "USER");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(testEmail));

        // 2. Login with the new user
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", testEmail);
        loginUser.put("password", testPassword);
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        // 3. Access protected endpoint without token (should fail)
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());

        // 4. Access protected endpoint with token (should succeed)
        mockMvc.perform(get("/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(testEmail));
    }

    @Test
    void testRegisterWithExistingEmailReturns409() throws Exception {
        // Register user first
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
        createUser.put("role", "USER");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated());
        // Try to register again with same email
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isConflict());
    }

    @Test
    void testLoginWithWrongPasswordReturns401() throws Exception {
        // Register user
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
        createUser.put("role", "USER");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated());
        // Try to login with wrong password
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", testEmail);
        loginUser.put("password", "wrongpassword");
        mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLoginWithNonExistentEmailReturns401() throws Exception {
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", "doesnotexist@example.com");
        loginUser.put("password", "irrelevant");
        mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessProtectedEndpointWithInvalidJWTReturns401() throws Exception {
        mockMvc.perform(get("/users")
                .header("Authorization", "Bearer invalidtoken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDeleteUserWithoutTokenReturns403() throws Exception {
        // Register user
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
        createUser.put("role", "USER");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated());
        // Try to delete without token
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isForbidden());
    }
} 