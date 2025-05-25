package com.fitlog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitlog.entity.Workout;
import com.fitlog.repository.WorkoutRepository;
import com.fitlog.repository.UserRepository;
import com.fitlog.repository.WorkoutExerciseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
// Use the test profile configuration (application-test.properties) to ensure tests run against H2, not the real database
@org.springframework.test.context.TestPropertySource(locations = "classpath:application-test.properties")
public class WorkoutControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private WorkoutExerciseRepository workoutExerciseRepository;

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

    @BeforeEach
    void cleanUp() {
        // Delete workout_exercises first to avoid foreign key constraint errors
        workoutExerciseRepository.deleteAll();
        workoutRepository.deleteAll();
    }

    @Test
    void userCanCreateWorkout() throws Exception {
        String email = registerUser("user");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        Map<String, Object> req = Map.of(
                "date", LocalDate.now().toString(),
                "notes", "Leg day"
        );
        mockMvc.perform(post("/workouts")
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notes").value("Leg day"));
    }

    @Test
    void userCannotCreateWorkoutWithoutAuth() throws Exception {
        Map<String, Object> req = Map.of(
                "date", LocalDate.now().toString(),
                "notes", "No auth"
        );
        mockMvc.perform(post("/workouts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanUpdateOwnWorkout() throws Exception {
        String email = registerUser("user");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create workout
        Workout workout = new Workout();
        workout.setDate(LocalDate.now());
        workout.setNotes("Initial");
        workout.setUser(userRepository.findByEmail(email).get());
        workout = workoutRepository.save(workout);
        Map<String, Object> req = Map.of(
                "date", LocalDate.now().plusDays(1).toString(),
                "notes", "Updated notes"
        );
        mockMvc.perform(put("/workouts/" + workout.getId())
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Updated notes"));
    }

    @Test
    void userCannotUpdateOthersWorkout() throws Exception {
        String email = registerUser("user");
        String otherEmail = registerUser("other");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create workout owned by other
        Workout workout = new Workout();
        workout.setDate(LocalDate.now());
        workout.setNotes("Other's workout");
        workout.setUser(userRepository.findByEmail(otherEmail).get());
        workout = workoutRepository.save(workout);
        Map<String, Object> req = Map.of(
                "date", LocalDate.now().toString(),
                "notes", "Should not update"
        );
        mockMvc.perform(put("/workouts/" + workout.getId())
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanDeleteOwnWorkout() throws Exception {
        String email = registerUser("user");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create workout
        Workout workout = new Workout();
        workout.setDate(LocalDate.now());
        workout.setNotes("To delete");
        workout.setUser(userRepository.findByEmail(email).get());
        workout = workoutRepository.save(workout);
        mockMvc.perform(delete("/workouts/" + workout.getId()).cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Workout deleted."));
    }

    @Test
    void userCannotDeleteOthersWorkout() throws Exception {
        String email = registerUser("user");
        String otherEmail = registerUser("other");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create workout owned by other
        Workout workout = new Workout();
        workout.setDate(LocalDate.now());
        workout.setNotes("Other's workout");
        workout.setUser(userRepository.findByEmail(otherEmail).get());
        workout = workoutRepository.save(workout);
        mockMvc.perform(delete("/workouts/" + workout.getId()).cookie(jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanGetOwnWorkouts() throws Exception {
        String email = registerUser("user");
        String otherEmail = registerUser("other");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create workout for user
        Workout own = new Workout();
        own.setDate(LocalDate.now());
        own.setNotes("Mine");
        own.setUser(userRepository.findByEmail(email).get());
        workoutRepository.save(own);
        // Create workout for other
        Workout other = new Workout();
        other.setDate(LocalDate.now());
        other.setNotes("Not mine");
        other.setUser(userRepository.findByEmail(otherEmail).get());
        workoutRepository.save(other);
        mockMvc.perform(get("/workouts").cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.notes=='Mine')]").exists())
                .andExpect(jsonPath("$[?(@.notes=='Not mine')]").doesNotExist());
    }

    @Test
    void userCanGetOwnWorkoutById() throws Exception {
        String email = registerUser("user");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create workout for user
        Workout own = new Workout();
        own.setDate(LocalDate.now());
        own.setNotes("Mine");
        own.setUser(userRepository.findByEmail(email).get());
        own = workoutRepository.save(own);
        mockMvc.perform(get("/workouts/" + own.getId()).cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Mine"));
    }

    @Test
    void userCannotGetOthersWorkoutById() throws Exception {
        String email = registerUser("user");
        String otherEmail = registerUser("other");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create workout for other
        Workout other = new Workout();
        other.setDate(LocalDate.now());
        other.setNotes("Not mine");
        other.setUser(userRepository.findByEmail(otherEmail).get());
        other = workoutRepository.save(other);
        mockMvc.perform(get("/workouts/" + other.getId()).cookie(jwt))
                .andExpect(status().isForbidden());
    }
} 