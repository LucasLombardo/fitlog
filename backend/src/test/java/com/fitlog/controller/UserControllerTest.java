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

import java.util.UUID;

import com.fitlog.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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

    // Utility to delete a user by email if exists
    void deleteUserByEmailIfExists(String email, String password) throws Exception {
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", email);
        loginUser.put("password", password);
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andReturn();
        if (loginResult.getResponse().getStatus() == 200) {
            String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
            MvcResult usersResult = mockMvc.perform(get("/users")
                    .header("Authorization", "Bearer " + token))
                    .andReturn();
            var users = objectMapper.readTree(usersResult.getResponse().getContentAsString());
            for (var user : users) {
                if (user.get("email").asText().equals(email)) {
                    long id = user.get("id").asLong();
                    mockMvc.perform(delete("/users/" + id)
                            .header("Authorization", "Bearer " + token));
                }
            }
        }
    }

    // Utility to generate a unique email for each test
    private String uniqueEmail(String base) {
        return base + "+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    @Test
    void testUserRegistrationAndLoginAndProtectedEndpoint() throws Exception {
        String testEmail = uniqueEmail("testuser");
        // 1. Register a new user
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
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

        // 4. Access protected endpoint with token (should fail for non-admin)
        mockMvc.perform(get("/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRegisterWithExistingEmailReturns409() throws Exception {
        String testEmail = uniqueEmail("testuser");
        // Register user first
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
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
        String testEmail = uniqueEmail("testuser");
        // Register user
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
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
        String testEmail = uniqueEmail("testuser");
        // Register user
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated());
        // Try to delete without token
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetUserById_AsSelf_Succeeds() throws Exception {
        String testEmail = uniqueEmail("testuser");
        // Register and login as a normal user
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated());
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", testEmail);
        loginUser.put("password", testPassword);
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        long userId = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("user").get("id").asLong();
        // User can get themselves
        mockMvc.perform(get("/users/" + userId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testEmail));
    }

    @Test
    void testGetUserById_AsAdmin_Succeeds() throws Exception {
        String testEmail = uniqueEmail("testuser");
        String adminEmail = uniqueEmail("admin");
        // Clean up users
        deleteUserByEmailIfExists(testEmail, testPassword);
        deleteUserByEmailIfExists("admin@example.com", "adminpass");
        // Register a normal user
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated());
        // Register and login as admin
        var createAdmin = new java.util.HashMap<String, String>();
        createAdmin.put("email", adminEmail);
        createAdmin.put("password", "adminpass");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAdmin)))
                .andExpect(status().isCreated());
        // Set admin role directly in DB for this test
        userRepository.findByEmail(adminEmail).ifPresent(user -> {
            user.setRole("ADMIN");
            userRepository.save(user);
        });
        var loginAdmin = new java.util.HashMap<String, String>();
        loginAdmin.put("email", adminEmail);
        loginAdmin.put("password", "adminpass");
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginAdmin)))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        // Get user id
        MvcResult usersResult = mockMvc.perform(get("/users")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        var users = objectMapper.readTree(usersResult.getResponse().getContentAsString());
        long userId = -1;
        for (var user : users) {
            if (user.get("email").asText().equals(testEmail)) {
                userId = user.get("id").asLong();
            }
        }
        // Admin can get any user
        mockMvc.perform(get("/users/" + userId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testEmail));
    }

    @Test
    void testGetUserById_AsOtherUser_Forbidden() throws Exception {
        String user1Email = uniqueEmail("user1");
        String user2Email = uniqueEmail("user2");
        // Clean up users
        deleteUserByEmailIfExists("user1@example.com", "user1pass");
        deleteUserByEmailIfExists("user2@example.com", "user2pass");
        // Register user1
        var createUser1 = new java.util.HashMap<String, String>();
        createUser1.put("email", user1Email);
        createUser1.put("password", "user1pass");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser1)))
                .andExpect(status().isCreated());
        // Register user2
        var createUser2 = new java.util.HashMap<String, String>();
        createUser2.put("email", user2Email);
        createUser2.put("password", "user2pass");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser2)))
                .andExpect(status().isCreated());
        // Login as user2
        var loginUser2 = new java.util.HashMap<String, String>();
        loginUser2.put("email", user2Email);
        loginUser2.put("password", "user2pass");
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser2)))
                .andExpect(status().isOk())
                .andReturn();
        String user2Token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        // Get user1 id
        var loginUser1 = new java.util.HashMap<String, String>();
        loginUser1.put("email", user1Email);
        loginUser1.put("password", "user1pass");
        MvcResult loginResult1 = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser1)))
                .andExpect(status().isOk())
                .andReturn();
        String user1Token = objectMapper.readTree(loginResult1.getResponse().getContentAsString()).get("token").asText();
        MvcResult usersResult = mockMvc.perform(get("/users")
                .header("Authorization", "Bearer " + user1Token))
                .andReturn();
        var users = objectMapper.readTree(usersResult.getResponse().getContentAsString());
        long user1Id = -1;
        for (var user : users) {
            if (user.get("email").asText().equals(user1Email)) {
                user1Id = user.get("id").asLong();
            }
        }
        // user2 cannot get user1
        mockMvc.perform(get("/users/" + user1Id)
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        String adminEmail = uniqueEmail("admin2");
        // Clean up admin
        deleteUserByEmailIfExists("admin2@example.com", "adminpass2");
        // Register and login as admin
        var createAdmin = new java.util.HashMap<String, String>();
        createAdmin.put("email", adminEmail);
        createAdmin.put("password", "adminpass2");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAdmin)))
                .andExpect(status().isCreated());
        // Set admin role directly in DB for this test
        userRepository.findByEmail(adminEmail).ifPresent(user -> {
            user.setRole("ADMIN");
            userRepository.save(user);
        });
        var loginAdmin = new java.util.HashMap<String, String>();
        loginAdmin.put("email", adminEmail);
        loginAdmin.put("password", "adminpass2");
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginAdmin)))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        // Try to get a non-existent user
        mockMvc.perform(get("/users/999999")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserById_MissingOrInvalidToken() throws Exception {
        // Should return 403 for missing token
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isForbidden());
        // Should return 401 for invalid token
        mockMvc.perform(get("/users/1")
                .header("Authorization", "Bearer invalidtoken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAllUsers_AsUser_Forbidden() throws Exception {
        String testEmail = uniqueEmail("testuser");
        // Register and login as a normal user
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated());
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", testEmail);
        loginUser.put("password", testPassword);
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        // User should not be able to get all users
        mockMvc.perform(get("/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAllUsers_AsAdmin_Succeeds() throws Exception {
        String adminEmail = uniqueEmail("admin3");
        // Register and login as admin
        var createAdmin = new java.util.HashMap<String, String>();
        createAdmin.put("email", adminEmail);
        createAdmin.put("password", "adminpass3");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAdmin)))
                .andExpect(status().isCreated());
        // Set admin role directly in DB for this test
        userRepository.findByEmail(adminEmail).ifPresent(user -> {
            user.setRole("ADMIN");
            userRepository.save(user);
        });
        var loginAdmin = new java.util.HashMap<String, String>();
        loginAdmin.put("email", adminEmail);
        loginAdmin.put("password", "adminpass3");
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginAdmin)))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        // Admin should be able to get all users
        mockMvc.perform(get("/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteUser_AsUser_Forbidden() throws Exception {
        String testEmail = uniqueEmail("testuser");
        // Register and login as a normal user
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated());
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", testEmail);
        loginUser.put("password", testPassword);
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        // User should not be able to delete any user
        mockMvc.perform(delete("/users/1")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteUser_AsAdmin_Succeeds() throws Exception {
        String adminEmail = uniqueEmail("admin4");
        String userEmail = uniqueEmail("deleteuser");
        // Register and login as admin
        var createAdmin = new java.util.HashMap<String, String>();
        createAdmin.put("email", adminEmail);
        createAdmin.put("password", "adminpass4");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAdmin)))
                .andExpect(status().isCreated());
        // Set admin role directly in DB for this test
        userRepository.findByEmail(adminEmail).ifPresent(user -> {
            user.setRole("ADMIN");
            userRepository.save(user);
        });
        var loginAdmin = new java.util.HashMap<String, String>();
        loginAdmin.put("email", adminEmail);
        loginAdmin.put("password", "adminpass4");
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginAdmin)))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        // Register a user to delete
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", userEmail);
        createUser.put("password", "deletepass");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated());
        // Get user id
        MvcResult usersResult = mockMvc.perform(get("/users")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        var users = objectMapper.readTree(usersResult.getResponse().getContentAsString());
        long userId = -1;
        for (var user : users) {
            if (user.get("email").asText().equals(userEmail)) {
                userId = user.get("id").asLong();
            }
        }
        // Admin can delete user
        mockMvc.perform(delete("/users/" + userId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteOwnAccount_Succeeds() throws Exception {
        String testEmail = uniqueEmail("selfdelete");
        // Register a new user
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated());
        // Login to get JWT
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", testEmail);
        loginUser.put("password", testPassword);
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        // Delete own account
        mockMvc.perform(delete("/users/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deleted successfully."));
        // Try to login again (should fail)
        mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isUnauthorized());
    }
} 