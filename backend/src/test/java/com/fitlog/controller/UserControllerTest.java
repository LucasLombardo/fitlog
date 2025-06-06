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
import org.springframework.mock.web.MockCookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import com.fitlog.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
// Use the test profile configuration (application-test.properties) to ensure tests run against H2, not the real database
@org.springframework.test.context.TestPropertySource(locations = "classpath:application-test.properties")
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String testEmail = "testuser@example.com";
    private String testPassword = "testpassword";

    // Helper to extract JWT from Set-Cookie header
    private String extractJwtFromSetCookie(MvcResult result) {
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        if (setCookie == null) return null;
        for (String cookie : setCookie.split(";")) {
            if (cookie.trim().startsWith("jwt=")) {
                return cookie.trim().substring(4);
            }
        }
        return null;
    }

    @BeforeEach
    void cleanUp() throws Exception {
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", testEmail);
        loginUser.put("password", testPassword);
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andReturn();
        if (loginResult.getResponse().getStatus() == 200) {
            String jwt = extractJwtFromSetCookie(loginResult);
            if (jwt != null) {
                MvcResult usersResult = mockMvc.perform(get("/users")
                        .cookie(new MockCookie("jwt", jwt)))
                        .andReturn();
                var users = objectMapper.readTree(usersResult.getResponse().getContentAsString());
                for (var user : users) {
                    if (user.get("email").asText().equals(testEmail)) {
                        UUID id = UUID.fromString(user.get("id").asText());
                        mockMvc.perform(delete("/users/" + id)
                                .cookie(new MockCookie("jwt", jwt)));
                    }
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
            String jwt = extractJwtFromSetCookie(loginResult);
            if (jwt != null) {
                MvcResult usersResult = mockMvc.perform(get("/users")
                        .cookie(new MockCookie("jwt", jwt)))
                        .andReturn();
                var users = objectMapper.readTree(usersResult.getResponse().getContentAsString());
                for (var user : users) {
                    if (user.get("email").asText().equals(email)) {
                        UUID id = UUID.fromString(user.get("id").asText());
                        mockMvc.perform(delete("/users/" + id)
                                .cookie(new MockCookie("jwt", jwt)));
                    }
                }
            }
        }
    }

    // Utility to generate a unique email for each test
    private String uniqueEmail(String base) {
        return base + "+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    // Helper to mark a user as verified for tests
    private void verifyUser(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setEmailVerified(true);
            userRepository.save(user);
        });
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
        verifyUser(testEmail);

        // 2. Login with the new user
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", testEmail);
        loginUser.put("password", testPassword);
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();
        String jwt = extractJwtFromSetCookie(loginResult);

        // 3. Access protected endpoint without token (should fail)
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());

        // 4. Access protected endpoint with cookie (should fail for non-admin)
        mockMvc.perform(get("/users")
                .cookie(new MockCookie("jwt", jwt)))
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
        verifyUser(testEmail);
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
        verifyUser(testEmail);
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
                .cookie(new MockCookie("jwt", "invalidtoken")))
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
        verifyUser(testEmail);
        // Try to delete without token
        mockMvc.perform(delete("/users/00000000-0000-0000-0000-000000000000"))
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
        verifyUser(testEmail);
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", testEmail);
        loginUser.put("password", testPassword);
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();
        String jwt = extractJwtFromSetCookie(loginResult);
        UUID userId = UUID.fromString(objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("user").get("id").asText());
        // User can get themselves
        mockMvc.perform(get("/users/" + userId)
                .cookie(new MockCookie("jwt", jwt)))
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
        verifyUser(testEmail);
        // Register and login as admin
        var createAdmin = new java.util.HashMap<String, String>();
        createAdmin.put("email", adminEmail);
        createAdmin.put("password", "adminpass");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAdmin)))
                .andExpect(status().isCreated());
        verifyUser(adminEmail);
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
        String adminJwt = extractJwtFromSetCookie(loginResult);
        // Get user id
        MvcResult usersResult = mockMvc.perform(get("/users")
                .cookie(new MockCookie("jwt", adminJwt)))
                .andReturn();
        var users = objectMapper.readTree(usersResult.getResponse().getContentAsString());
        UUID userId = null;
        for (var user : users) {
            if (user.get("email").asText().equals(testEmail)) {
                userId = UUID.fromString(user.get("id").asText());
            }
        }
        // Admin can get any user
        mockMvc.perform(get("/users/" + userId)
                .cookie(new MockCookie("jwt", adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testEmail));
    }

    @Test
    void testGetUserById_AsOtherUser_Forbidden() throws Exception {
        String user1Email = uniqueEmail("user1");
        String user2Email = uniqueEmail("user2");
        // Clean up users
        deleteUserByEmailIfExists(user1Email, "user1pass");
        deleteUserByEmailIfExists(user2Email, "user2pass");
        // Register user1
        var createUser1 = new java.util.HashMap<String, String>();
        createUser1.put("email", user1Email);
        createUser1.put("password", "user1pass");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser1)))
                .andExpect(status().isCreated());
        verifyUser(user1Email);
        // Register user2
        var createUser2 = new java.util.HashMap<String, String>();
        createUser2.put("email", user2Email);
        createUser2.put("password", "user2pass");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser2)))
                .andExpect(status().isCreated());
        verifyUser(user2Email);
        // Login as user2
        var loginUser2 = new java.util.HashMap<String, String>();
        loginUser2.put("email", user2Email);
        loginUser2.put("password", "user2pass");
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser2)))
                .andExpect(status().isOk())
                .andReturn();
        String user2Jwt = extractJwtFromSetCookie(loginResult);
        // Login as user1 and get user1Id from login response
        var loginUser1 = new java.util.HashMap<String, String>();
        loginUser1.put("email", user1Email);
        loginUser1.put("password", "user1pass");
        MvcResult loginResult1 = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser1)))
                .andExpect(status().isOk())
                .andReturn();
        UUID user1Id = UUID.fromString(objectMapper.readTree(loginResult1.getResponse().getContentAsString()).get("user").get("id").asText());
        // user2 cannot get user1
        mockMvc.perform(get("/users/" + user1Id)
                .cookie(new MockCookie("jwt", user2Jwt)))
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
        verifyUser(adminEmail);
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
        String adminJwt = extractJwtFromSetCookie(loginResult);
        // Try to get a non-existent user
        mockMvc.perform(get("/users/00000000-0000-0000-0000-000000000000")
                .cookie(new MockCookie("jwt", adminJwt)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserById_MissingOrInvalidToken() throws Exception {
        // Should return 403 for missing token
        mockMvc.perform(get("/users/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isForbidden());
        // Should return 401 for invalid token
        mockMvc.perform(get("/users/00000000-0000-0000-0000-000000000000")
                .cookie(new MockCookie("jwt", "invalidtoken")))
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
        verifyUser(testEmail);
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", testEmail);
        loginUser.put("password", testPassword);
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();
        String jwt = extractJwtFromSetCookie(loginResult);
        // User should not be able to get all users
        mockMvc.perform(get("/users")
                .cookie(new MockCookie("jwt", jwt)))
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
        verifyUser(adminEmail);
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
        String jwt = extractJwtFromSetCookie(loginResult);
        // Admin should be able to get all users
        mockMvc.perform(get("/users")
                .cookie(new MockCookie("jwt", jwt)))
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
        verifyUser(testEmail);
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", testEmail);
        loginUser.put("password", testPassword);
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();
        String jwt = extractJwtFromSetCookie(loginResult);
        // User should not be able to delete any user
        mockMvc.perform(delete("/users/00000000-0000-0000-0000-000000000000")
                .cookie(new MockCookie("jwt", jwt)))
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
        verifyUser(adminEmail);
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
        String jwt = extractJwtFromSetCookie(loginResult);
        // Register a user to delete
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", userEmail);
        createUser.put("password", "deletepass");
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated());
        verifyUser(userEmail);
        // Get user id
        MvcResult usersResult = mockMvc.perform(get("/users")
                .cookie(new MockCookie("jwt", jwt)))
                .andReturn();
        var users = objectMapper.readTree(usersResult.getResponse().getContentAsString());
        UUID userId = null;
        for (var user : users) {
            if (user.get("email").asText().equals(userEmail)) {
                userId = UUID.fromString(user.get("id").asText());
            }
        }
        // Admin can delete user
        mockMvc.perform(delete("/users/" + userId)
                .cookie(new MockCookie("jwt", jwt)))
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
        verifyUser(testEmail);
        // Login to get JWT
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", testEmail);
        loginUser.put("password", testPassword);
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();
        String jwt = extractJwtFromSetCookie(loginResult);
        // Delete own account
        mockMvc.perform(delete("/users/me")
                .cookie(new MockCookie("jwt", jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deleted successfully."));
        // Try to login again (should fail)
        mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogoutRemovesJwtCookieAndPreventsAccess() throws Exception {
        // 1. Register a new user
        String testEmail = uniqueEmail("logoutuser");
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated());
        verifyUser(testEmail);

        // 2. Login to get JWT cookie
        var loginUser = new java.util.HashMap<String, String>();
        loginUser.put("email", testEmail);
        loginUser.put("password", testPassword);
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();
        String jwt = extractJwtFromSetCookie(loginResult);
        MockCookie jwtCookie = new MockCookie("jwt", jwt);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);

        // 3. Call /users/logout with the JWT cookie
        MvcResult logoutResult = mockMvc.perform(post("/users/logout")
                .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful. JWT cookie removed."))
                .andReturn();
        // 4. Assert the Set-Cookie header removes the JWT
        String setCookie = logoutResult.getResponse().getHeader("Set-Cookie");
        // Should set jwt=; Max-Age=0 (expired)
        assert setCookie != null && setCookie.contains("jwt=") && setCookie.contains("Max-Age=0");

        // 5. Try to access a protected endpoint with the old JWT (should fail)
        mockMvc.perform(get("/users")
                .cookie(jwtCookie))
                .andExpect(status().isForbidden()); // Not admin, so forbidden
    }

    @Test
    void testVerifyEmailEndpoint_Succeeds() throws Exception {
        String testEmail = uniqueEmail("verifyemail");
        // Register a new user
        var createUser = new java.util.HashMap<String, String>();
        createUser.put("email", testEmail);
        createUser.put("password", testPassword);
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated());
        // Fetch the user and get the verification code
        var userOpt = userRepository.findByEmail(testEmail);
        assert(userOpt.isPresent());
        var user = userOpt.get();
        String code = user.getEmailVerificationCode();
        // Call verify-email endpoint
        var verifyRequest = new java.util.HashMap<String, String>();
        verifyRequest.put("email", testEmail);
        verifyRequest.put("code", code);
        mockMvc.perform(post("/users/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully. You can now log in."));
        // Check user is now verified
        var verifiedUser = userRepository.findByEmail(testEmail).get();
        assert(verifiedUser.isEmailVerified());
    }
} 