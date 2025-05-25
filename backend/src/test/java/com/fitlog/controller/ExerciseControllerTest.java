package com.fitlog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitlog.entity.Exercise;
import com.fitlog.repository.ExerciseRepository;
import com.fitlog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ExerciseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

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

    // Helper to register a user and return their email
    private String registerUser(String base) throws Exception {
        String email = base + "+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        var createUser = new HashMap<String, String>();
        createUser.put("email", email);
        createUser.put("password", testPassword);
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isCreated());
        return email;
    }

    // Helper to login and get JWT cookie
    private MockCookie loginAndGetJwtCookie(String email, String password) throws Exception {
        var loginUser = new HashMap<String, String>();
        loginUser.put("email", email);
        loginUser.put("password", password);
        MvcResult loginResult = mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();
        String jwt = extractJwtFromSetCookie(loginResult);
        return new MockCookie("jwt", jwt);
    }

    // Helper to set admin role for a user
    private void setAdminRole(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setRole("ADMIN");
            userRepository.save(user);
        });
    }

    @BeforeEach
    void cleanUp() {
        exerciseRepository.deleteAll();
    }

    @Test
    void userCanCreateOwnPrivateExercise() throws Exception {
        String email = registerUser("user");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        Map<String, Object> req = Map.of(
                "name", "Pushup",
                "isPublic", false,
                "muscleGroups", "Chest",
                "notes", "Bodyweight"
        );
        mockMvc.perform(post("/exercises")
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pushup"))
                .andExpect(jsonPath("$.isPublic").value(false));
    }

    @Test
    void userCannotCreatePublicExercise() throws Exception {
        String email = registerUser("user");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        Map<String, Object> req = Map.of(
                "name", "Pullup",
                "isPublic", true,
                "muscleGroups", "Back",
                "notes", "Bar"
        );
        mockMvc.perform(post("/exercises")
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreatePublicExercise() throws Exception {
        String email = registerUser("admin");
        setAdminRole(email);
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        Map<String, Object> req = Map.of(
                "name", "Squat",
                "isPublic", true,
                "muscleGroups", "Legs",
                "notes", "Barbell"
        );
        mockMvc.perform(post("/exercises")
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPublic").value(true));
    }

    @Test
    void userCanUpdateOwnExercise() throws Exception {
        String email = registerUser("user");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create exercise
        Exercise ex = new Exercise();
        ex.setName("Situp");
        ex.setPublic(false);
        ex.setActive(true);
        ex.setMuscleGroups("Abs");
        ex.setNotes("Mat");
        ex.setCreatedBy(userRepository.findByEmail(email).get());
        ex = exerciseRepository.save(ex);
        Map<String, Object> req = Map.of(
                "name", "Situp",
                "isPublic", false,
                "muscleGroups", "Abs",
                "notes", "Mat"
        );
        mockMvc.perform(put("/exercises/" + ex.getId())
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Situp"));
    }

    @Test
    void userCannotUpdateOthersExercise() throws Exception {
        String email = registerUser("user");
        String adminEmail = registerUser("admin");
        setAdminRole(adminEmail);
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create exercise owned by admin
        Exercise ex = new Exercise();
        ex.setName("Situp");
        ex.setPublic(false);
        ex.setActive(true);
        ex.setMuscleGroups("Abs");
        ex.setNotes("Mat");
        ex.setCreatedBy(userRepository.findByEmail(adminEmail).get());
        ex = exerciseRepository.save(ex);
        Map<String, Object> req = Map.of(
                "name", "Situp",
                "isPublic", false,
                "muscleGroups", "Abs",
                "notes", "Mat"
        );
        mockMvc.perform(put("/exercises/" + ex.getId())
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUpdateAnyExercise() throws Exception {
        String email = registerUser("user");
        String adminEmail = registerUser("admin");
        setAdminRole(adminEmail);
        MockCookie jwt = loginAndGetJwtCookie(adminEmail, testPassword);
        // Create exercise owned by user
        Exercise ex = new Exercise();
        ex.setName("Situp");
        ex.setPublic(false);
        ex.setActive(true);
        ex.setMuscleGroups("Abs");
        ex.setNotes("Mat");
        ex.setCreatedBy(userRepository.findByEmail(email).get());
        ex = exerciseRepository.save(ex);
        Map<String, Object> req = Map.of(
                "name", "Situp",
                "isPublic", true,
                "muscleGroups", "Abs",
                "notes", "Mat"
        );
        mockMvc.perform(put("/exercises/" + ex.getId())
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(true));
    }

    @Test
    void getExercises_userGetsPublicAndOwn() throws Exception {
        String email = registerUser("user");
        String adminEmail = registerUser("admin");
        setAdminRole(adminEmail);
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create public exercise by admin
        Exercise pub = new Exercise();
        pub.setName("Jumping Jacks");
        pub.setPublic(true);
        pub.setActive(true);
        pub.setCreatedBy(userRepository.findByEmail(adminEmail).get());
        exerciseRepository.save(pub);
        // Create private exercise by user
        Exercise own = new Exercise();
        own.setName("Crunch");
        own.setPublic(false);
        own.setActive(true);
        own.setCreatedBy(userRepository.findByEmail(email).get());
        exerciseRepository.save(own);
        mockMvc.perform(get("/exercises").cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Jumping Jacks')]").exists())
                .andExpect(jsonPath("$[?(@.name=='Crunch')]").exists());
    }

    @Test
    void getExercises_adminGetsAll() throws Exception {
        String email = registerUser("user");
        String adminEmail = registerUser("admin");
        setAdminRole(adminEmail);
        MockCookie jwt = loginAndGetJwtCookie(adminEmail, testPassword);
        // Create public exercise by admin
        Exercise pub = new Exercise();
        pub.setName("Jumping Jacks");
        pub.setPublic(true);
        pub.setActive(true);
        pub.setCreatedBy(userRepository.findByEmail(adminEmail).get());
        exerciseRepository.save(pub);
        // Create private exercise by user
        Exercise own = new Exercise();
        own.setName("Crunch");
        own.setPublic(false);
        own.setActive(true);
        own.setCreatedBy(userRepository.findByEmail(email).get());
        exerciseRepository.save(own);
        mockMvc.perform(get("/exercises").cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Jumping Jacks')]").exists())
                .andExpect(jsonPath("$[?(@.name=='Crunch')]").exists());
    }

    @Test
    void getSingleExercise_userCanGetPublicOrOwn() throws Exception {
        String email = registerUser("user");
        String adminEmail = registerUser("admin");
        setAdminRole(adminEmail);
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create public exercise by admin
        Exercise pub = new Exercise();
        pub.setName("Jumping Jacks");
        pub.setPublic(true);
        pub.setActive(true);
        pub.setCreatedBy(userRepository.findByEmail(adminEmail).get());
        pub = exerciseRepository.save(pub);
        // Create private exercise by user
        Exercise own = new Exercise();
        own.setName("Crunch");
        own.setPublic(false);
        own.setActive(true);
        own.setCreatedBy(userRepository.findByEmail(email).get());
        own = exerciseRepository.save(own);
        mockMvc.perform(get("/exercises/" + pub.getId()).cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jumping Jacks"));
        mockMvc.perform(get("/exercises/" + own.getId()).cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Crunch"));
    }

    @Test
    void getSingleExercise_userCannotGetOthersPrivate() throws Exception {
        String email = registerUser("user");
        String adminEmail = registerUser("admin");
        setAdminRole(adminEmail);
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create private exercise by admin
        Exercise ex = new Exercise();
        ex.setName("Crunch");
        ex.setPublic(false);
        ex.setActive(true);
        ex.setCreatedBy(userRepository.findByEmail(adminEmail).get());
        ex = exerciseRepository.save(ex);
        mockMvc.perform(get("/exercises/" + ex.getId()).cookie(jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteExercise_userCanDeleteOwn() throws Exception {
        String email = registerUser("user");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create exercise
        Exercise ex = new Exercise();
        ex.setName("Situp");
        ex.setPublic(false);
        ex.setActive(true);
        ex.setCreatedBy(userRepository.findByEmail(email).get());
        ex = exerciseRepository.save(ex);
        mockMvc.perform(delete("/exercises/" + ex.getId()).cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Exercise deleted (soft)."));
    }

    @Test
    void deleteExercise_userCannotDeleteOthers() throws Exception {
        String email = registerUser("user");
        String adminEmail = registerUser("admin");
        setAdminRole(adminEmail);
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create exercise owned by admin
        Exercise ex = new Exercise();
        ex.setName("Situp");
        ex.setPublic(false);
        ex.setActive(true);
        ex.setCreatedBy(userRepository.findByEmail(adminEmail).get());
        ex = exerciseRepository.save(ex);
        mockMvc.perform(delete("/exercises/" + ex.getId()).cookie(jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteExercise_adminCanDeleteAny() throws Exception {
        String email = registerUser("user");
        String adminEmail = registerUser("admin");
        setAdminRole(adminEmail);
        MockCookie jwt = loginAndGetJwtCookie(adminEmail, testPassword);
        // Create exercise owned by user
        Exercise ex = new Exercise();
        ex.setName("Situp");
        ex.setPublic(false);
        ex.setActive(true);
        ex.setCreatedBy(userRepository.findByEmail(email).get());
        ex = exerciseRepository.save(ex);
        mockMvc.perform(delete("/exercises/" + ex.getId()).cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Exercise deleted (soft)."));
    }
} 