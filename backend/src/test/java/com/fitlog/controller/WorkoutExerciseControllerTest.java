package com.fitlog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitlog.entity.Workout;
import com.fitlog.entity.Exercise;
import com.fitlog.entity.WorkoutExercise;
import com.fitlog.repository.WorkoutRepository;
import com.fitlog.repository.ExerciseRepository;
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
public class WorkoutExerciseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

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
        workoutExerciseRepository.deleteAll();
        workoutRepository.deleteAll();
        exerciseRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Helper to create a workout for a user
    private Workout createWorkout(String email) {
        Workout workout = new Workout();
        workout.setDate(LocalDate.now());
        workout.setNotes("Workout for " + email);
        workout.setUser(userRepository.findByEmail(email).get());
        return workoutRepository.save(workout);
    }

    // Helper to create an exercise for a user
    private Exercise createExercise(String email) {
        Exercise exercise = new Exercise();
        exercise.setName("Pushup-" + UUID.randomUUID());
        exercise.setPublic(true);
        exercise.setMuscleGroups("Chest");
        exercise.setNotes("Test exercise");
        exercise.setActive(true);
        exercise.setCreatedBy(userRepository.findByEmail(email).get());
        return exerciseRepository.save(exercise);
    }

    @Test
    void userCanCreateWorkoutExercise() throws Exception {
        String email = registerUser("user");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        Workout workout = createWorkout(email);
        Exercise exercise = createExercise(email);
        Map<String, Object> req = Map.of(
                "workoutId", workout.getId(),
                "exerciseId", exercise.getId(),
                "position", 1,
                "sets", "[{\"reps\":10,\"weight\":0}]",
                "notes", "First set"
        );
        mockMvc.perform(post("/workout_exercises")
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notes").value("First set"));
    }

    @Test
    void userCannotCreateWorkoutExerciseForOthersWorkout() throws Exception {
        String email = registerUser("user");
        String otherEmail = registerUser("other");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        Workout workout = createWorkout(otherEmail);
        Exercise exercise = createExercise(email);
        Map<String, Object> req = Map.of(
                "workoutId", workout.getId(),
                "exerciseId", exercise.getId(),
                "position", 1,
                "sets", "[{\"reps\":10,\"weight\":0}]",
                "notes", "Should not create"
        );
        mockMvc.perform(post("/workout_exercises")
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanUpdateOwnWorkoutExercise() throws Exception {
        String email = registerUser("user");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        Workout workout = createWorkout(email);
        Exercise exercise = createExercise(email);
        // Create workout exercise
        WorkoutExercise we = new WorkoutExercise();
        we.setWorkout(workout);
        we.setExercise(exercise);
        we.setPosition(1);
        we.setSets("[{\"reps\":10,\"weight\":0}]");
        we.setNotes("Initial");
        we = workoutExerciseRepository.save(we);
        Map<String, Object> req = Map.of(
                "exerciseId", exercise.getId(),
                "position", 2,
                "sets", "[{\"reps\":12,\"weight\":0}]",
                "notes", "Updated notes"
        );
        mockMvc.perform(put("/workout_exercises/" + we.getId())
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Updated notes"))
                .andExpect(jsonPath("$.position").value(2));
    }

    @Test
    void userCannotUpdateOthersWorkoutExercise() throws Exception {
        String email = registerUser("user");
        String otherEmail = registerUser("other");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        Workout workout = createWorkout(otherEmail);
        Exercise exercise = createExercise(otherEmail);
        // Create workout exercise owned by other
        WorkoutExercise we = new WorkoutExercise();
        we.setWorkout(workout);
        we.setExercise(exercise);
        we.setPosition(1);
        we.setSets("[{\"reps\":10,\"weight\":0}]");
        we.setNotes("Other's");
        we = workoutExerciseRepository.save(we);
        Map<String, Object> req = Map.of(
                "exerciseId", exercise.getId(),
                "position", 2,
                "sets", "[{\"reps\":12,\"weight\":0}]",
                "notes", "Should not update"
        );
        mockMvc.perform(put("/workout_exercises/" + we.getId())
                .cookie(jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanDeleteOwnWorkoutExercise() throws Exception {
        String email = registerUser("user");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        Workout workout = createWorkout(email);
        Exercise exercise = createExercise(email);
        // Create workout exercise
        WorkoutExercise we = new WorkoutExercise();
        we.setWorkout(workout);
        we.setExercise(exercise);
        we.setPosition(1);
        we.setSets("[{\"reps\":10,\"weight\":0}]");
        we.setNotes("To delete");
        we = workoutExerciseRepository.save(we);
        mockMvc.perform(delete("/workout_exercises/" + we.getId()).cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Workout exercise deleted."));
    }

    @Test
    void userCannotDeleteOthersWorkoutExercise() throws Exception {
        String email = registerUser("user");
        String otherEmail = registerUser("other");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        Workout workout = createWorkout(otherEmail);
        Exercise exercise = createExercise(otherEmail);
        // Create workout exercise owned by other
        WorkoutExercise we = new WorkoutExercise();
        we.setWorkout(workout);
        we.setExercise(exercise);
        we.setPosition(1);
        we.setSets("[{\"reps\":10,\"weight\":0}]");
        we.setNotes("Other's");
        we = workoutExerciseRepository.save(we);
        mockMvc.perform(delete("/workout_exercises/" + we.getId()).cookie(jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanGetWorkoutExercisesByWorkout() throws Exception {
        String email = registerUser("user");
        String otherEmail = registerUser("other");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        Workout workout = createWorkout(email);
        Workout otherWorkout = createWorkout(otherEmail);
        Exercise exercise = createExercise(email);
        // Create workout exercise for user
        WorkoutExercise we = new WorkoutExercise();
        we.setWorkout(workout);
        we.setExercise(exercise);
        we.setPosition(1);
        we.setSets("[{\"reps\":10,\"weight\":0}]");
        we.setNotes("Mine");
        workoutExerciseRepository.save(we);
        // Create workout exercise for other
        WorkoutExercise otherWe = new WorkoutExercise();
        otherWe.setWorkout(otherWorkout);
        otherWe.setExercise(exercise);
        otherWe.setPosition(1);
        otherWe.setSets("[{\"reps\":10,\"weight\":0}]");
        otherWe.setNotes("Not mine");
        workoutExerciseRepository.save(otherWe);
        mockMvc.perform(get("/workout_exercises/by_workout/" + workout.getId()).cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.notes=='Mine')]").exists())
                .andExpect(jsonPath("$[?(@.notes=='Not mine')]").doesNotExist());
    }

    @Test
    void userCanGetSingleWorkoutExerciseById() throws Exception {
        String email = registerUser("user");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        Workout workout = createWorkout(email);
        Exercise exercise = createExercise(email);
        // Create workout exercise
        WorkoutExercise we = new WorkoutExercise();
        we.setWorkout(workout);
        we.setExercise(exercise);
        we.setPosition(1);
        we.setSets("[{\"reps\":10,\"weight\":0}]");
        we.setNotes("Mine");
        we = workoutExerciseRepository.save(we);
        mockMvc.perform(get("/workout_exercises/" + we.getId()).cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Mine"));
    }

    @Test
    void userCannotGetOthersWorkoutExerciseById() throws Exception {
        String email = registerUser("user");
        String otherEmail = registerUser("other");
        MockCookie jwt = loginAndGetJwtCookie(email, testPassword);
        Workout workout = createWorkout(otherEmail);
        Exercise exercise = createExercise(otherEmail);
        // Create workout exercise for other
        WorkoutExercise we = new WorkoutExercise();
        we.setWorkout(workout);
        we.setExercise(exercise);
        we.setPosition(1);
        we.setSets("[{\"reps\":10,\"weight\":0}]");
        we.setNotes("Not mine");
        we = workoutExerciseRepository.save(we);
        mockMvc.perform(get("/workout_exercises/" + we.getId()).cookie(jwt))
                .andExpect(status().isForbidden());
    }
} 