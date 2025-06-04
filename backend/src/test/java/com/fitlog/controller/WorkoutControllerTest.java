package com.fitlog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitlog.entity.Workout;
import com.fitlog.repository.WorkoutRepository;
import com.fitlog.repository.UserRepository;
import com.fitlog.repository.WorkoutExerciseRepository;
import com.fitlog.entity.Exercise;
import com.fitlog.repository.ExerciseRepository;
import com.fitlog.entity.WorkoutExercise;
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
        // Mark user as verified for tests
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setEmailVerified(true);
            userRepository.save(user);
        });
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
        own = workoutRepository.save(own);
        // Create workout for other
        Workout other = new Workout();
        other.setDate(LocalDate.now());
        other.setNotes("Not mine");
        other.setUser(userRepository.findByEmail(otherEmail).get());
        workoutRepository.save(other);
        mockMvc.perform(get("/workouts").cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.notes=='Mine')]").exists())
                .andExpect(jsonPath("$[?(@.notes=='Not mine')]").doesNotExist())
                .andExpect(jsonPath("$[0].exercises").isArray());
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
                .andExpect(jsonPath("$.notes").value("Mine"))
                .andExpect(jsonPath("$.exercises").isArray());
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

    @Test
    void getWorkoutWithExercisesReturnsNestedData() throws Exception {
        String email = registerUser("user");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        // Create workout
        Workout workout = new Workout();
        workout.setDate(LocalDate.now());
        workout.setNotes("With exercises");
        workout.setUser(userRepository.findByEmail(email).get());
        workout = workoutRepository.save(workout);
        // Create exercise
        Exercise exercise = new Exercise();
        exercise.setName("Bench Press" + UUID.randomUUID());
        exercise.setMuscleGroups("Chest");
        exercise.setPublic(true);
        exercise.setActive(true);
        exercise.setNotes("Test exercise");
        exercise.setCreatedBy(userRepository.findByEmail(email).get());
        exercise = exerciseRepository.save(exercise);
        // Create workout exercise
        WorkoutExercise we = new WorkoutExercise();
        we.setWorkout(workout);
        we.setExercise(exercise);
        we.setPosition(1);
        we.setSets("[{\"reps\":10,\"weight\":100}]");
        we.setNotes("First set");
        workoutExerciseRepository.save(we);
        // GET all workouts
        mockMvc.perform(get("/workouts").cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exercises").isArray())
                .andExpect(jsonPath("$[0].exercises[0].exercise.name").value(exercise.getName()));
        // GET single workout
        mockMvc.perform(get("/workouts/" + workout.getId()).cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises").isArray())
                .andExpect(jsonPath("$.exercises[0].exercise.name").value(exercise.getName()));
    }

    @Test
    void userCannotCreateDuplicateWorkoutForSameDate() throws Exception {
        // Register and login user
        String email = registerUser("dupuser");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        String date = LocalDate.now().toString();
        Map<String, Object> req = Map.of(
                "date", date,
                "notes", "First workout"
        );
        // Create first workout
        MvcResult result1 = mockMvc.perform(post("/workouts")
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        String firstId = objectMapper.readTree(result1.getResponse().getContentAsString()).get("id").asText();
        // Try to create another workout with the same date
        Map<String, Object> req2 = Map.of(
                "date", date,
                "notes", "Second workout attempt"
        );
        MvcResult result2 = mockMvc.perform(post("/workouts")
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();
        String secondId = objectMapper.readTree(result2.getResponse().getContentAsString()).get("id").asText();
        // The id should be the same, meaning the same workout is returned
        org.junit.jupiter.api.Assertions.assertEquals(firstId, secondId, "Should return the same workout for duplicate date");
    }
} 