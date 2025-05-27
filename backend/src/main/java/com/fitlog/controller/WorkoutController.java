package com.fitlog.controller;

import com.fitlog.entity.Workout;
import com.fitlog.entity.User;
import com.fitlog.repository.WorkoutRepository;
import com.fitlog.repository.UserRepository;
import com.fitlog.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import com.fitlog.entity.WorkoutExercise;
import com.fitlog.entity.Exercise;
import com.fitlog.repository.WorkoutExerciseRepository;

// Controller for workout-related endpoints
@Tag(name = "Workout", description = "Operations related to workouts.")
@RestController
@RequestMapping("/workouts")
public class WorkoutController {
    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final WorkoutExerciseRepository workoutExerciseRepository;

    @Autowired
    public WorkoutController(WorkoutRepository workoutRepository, UserRepository userRepository, JwtUtil jwtUtil, WorkoutExerciseRepository workoutExerciseRepository) {
        this.workoutRepository = workoutRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.workoutExerciseRepository = workoutExerciseRepository;
    }

    // Helper method to extract user info from JWT (from header or cookie)
    private Optional<UserInfo> getUserInfo(String authHeader, HttpServletRequest request) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        if (token == null) return Optional.empty();
        try {
            var claims = jwtUtil.validateToken(token);
            UUID userId = UUID.fromString(claims.get("userId", String.class));
            return Optional.of(new UserInfo(userId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Simple DTO for user info
    private static class UserInfo {
        UUID userId;
        UserInfo(UUID userId) { this.userId = userId; }
    }

    // DTO for creating/updating workouts
    public static class WorkoutRequest {
        public String date; // ISO format (yyyy-MM-dd)
        public String notes;
    }

    // DTO for Exercise (to avoid exposing entity directly)
    public static class ExerciseDTO {
        public UUID id;
        public String name;
        public String muscleGroups;
        public boolean isPublic;
        public boolean isActive;
        public String notes;
        public ExerciseDTO(Exercise e) {
            this.id = e.getId();
            this.name = e.getName();
            this.muscleGroups = e.getMuscleGroups();
            this.isPublic = e.isPublic();
            this.isActive = e.isActive();
            this.notes = e.getNotes();
        }
    }

    // DTO for WorkoutExercise (to avoid exposing entity directly)
    public static class WorkoutExerciseDTO {
        public UUID id;
        public int position;
        public String sets;
        public String notes;
        public ExerciseDTO exercise;
        public WorkoutExerciseDTO(WorkoutExercise we) {
            this.id = we.getId();
            this.position = we.getPosition();
            this.sets = we.getSets();
            this.notes = we.getNotes();
            this.exercise = new ExerciseDTO(we.getExercise());
        }
    }

    // DTO for Workout with exercises
    public static class WorkoutWithExercisesDTO {
        public UUID id;
        public String date;
        public String notes;
        public String createdAt;
        public String updatedAt;
        public List<WorkoutExerciseDTO> exercises;
        public WorkoutWithExercisesDTO(Workout w, List<WorkoutExercise> wes) {
            this.id = w.getId();
            this.date = w.getDate().toString();
            this.notes = w.getNotes();
            this.createdAt = w.getCreatedAt().toString();
            this.updatedAt = w.getUpdatedAt().toString();
            this.exercises = wes.stream().map(WorkoutExerciseDTO::new).toList();
        }
    }

    /**
     * Create a new workout. Only authenticated users can create.
     */
    @Operation(summary = "Create workout", description = "Create a new workout owned by the current user.")
    @PostMapping
    public ResponseEntity<?> createWorkout(
            @RequestBody WorkoutRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        UserInfo userInfo = userInfoOpt.get();
        Optional<User> userOpt = userRepository.findById(userInfo.userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found."));
        }
        User user = userOpt.get();
        LocalDate workoutDate;
        try {
            // Parse the date from the request
            workoutDate = LocalDate.parse(request.date);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid date format. Use yyyy-MM-dd."));
        }
        // Check if a workout already exists for this user and date
        Optional<Workout> existingWorkoutOpt = workoutRepository.findByUserIdAndDate(user.getId(), workoutDate);
        if (existingWorkoutOpt.isPresent()) {
            // If a workout exists for this date, return it with 201 Created
            Workout existingWorkout = existingWorkoutOpt.get();
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", existingWorkout.getId(),
                "date", existingWorkout.getDate(),
                "notes", existingWorkout.getNotes(),
                "createdAt", existingWorkout.getCreatedAt(),
                "updatedAt", existingWorkout.getUpdatedAt()
            ));
        }
        // Otherwise, create a new workout for this date
        Workout workout = new Workout();
        workout.setDate(workoutDate);
        workout.setNotes(request.notes);
        workout.setUser(user);
        workoutRepository.save(workout);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", workout.getId(),
                "date", workout.getDate(),
                "notes", workout.getNotes(),
                "createdAt", workout.getCreatedAt(),
                "updatedAt", workout.getUpdatedAt()
        ));
    }

    /**
     * Update a workout. Only the owner can update.
     */
    @Operation(summary = "Update workout", description = "Update a workout. Only the owner can update.")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkout(
            @PathVariable UUID id,
            @RequestBody WorkoutRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        UserInfo userInfo = userInfoOpt.get();
        Optional<Workout> workoutOpt = workoutRepository.findById(id);
        if (workoutOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Workout not found."));
        }
        Workout workout = workoutOpt.get();
        // Only the owner can update
        if (!workout.getUser().getId().equals(userInfo.userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can only update your own workouts."));
        }
        try {
            workout.setDate(LocalDate.parse(request.date));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid date format. Use yyyy-MM-dd."));
        }
        workout.setNotes(request.notes);
        workoutRepository.save(workout);
        return ResponseEntity.ok(Map.of(
                "id", workout.getId(),
                "date", workout.getDate(),
                "notes", workout.getNotes(),
                "createdAt", workout.getCreatedAt(),
                "updatedAt", workout.getUpdatedAt()
        ));
    }

    /**
     * Delete a workout (hard delete). Only the owner can delete.
     */
    @Operation(summary = "Delete workout", description = "Hard delete a workout. Only the owner can delete.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkout(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        UserInfo userInfo = userInfoOpt.get();
        Optional<Workout> workoutOpt = workoutRepository.findById(id);
        if (workoutOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Workout not found."));
        }
        Workout workout = workoutOpt.get();
        // Only the owner can delete
        if (!workout.getUser().getId().equals(userInfo.userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can only delete your own workouts."));
        }
        workoutRepository.delete(workout); // Hard delete
        return ResponseEntity.ok(Map.of("message", "Workout deleted."));
    }

    /**
     * Get all workouts for the current user, including exercises and their details.
     */
    @Operation(summary = "Get all workouts", description = "Get all workouts associated with the current user, including exercises.")
    @GetMapping
    public ResponseEntity<?> getWorkouts(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        UserInfo userInfo = userInfoOpt.get();
        List<Workout> workouts = workoutRepository.findByUserId(userInfo.userId);
        // For each workout, fetch its exercises
        List<WorkoutWithExercisesDTO> result = workouts.stream().map(w -> {
            List<WorkoutExercise> wes = workoutExerciseRepository.findByWorkoutId(w.getId());
            return new WorkoutWithExercisesDTO(w, wes);
        }).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * Get a single workout by ID, including exercises and their details. Must be owned by current user.
     */
    @Operation(summary = "Get single workout", description = "Get a single workout by ID, including exercises. Must be owned by current user.")
    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkoutById(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        UserInfo userInfo = userInfoOpt.get();
        Optional<Workout> workoutOpt = workoutRepository.findById(id);
        if (workoutOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Workout not found."));
        }
        Workout workout = workoutOpt.get();
        if (!workout.getUser().getId().equals(userInfo.userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You are not allowed to access this workout."));
        }
        List<WorkoutExercise> wes = workoutExerciseRepository.findByWorkoutId(workout.getId());
        return ResponseEntity.ok(new WorkoutWithExercisesDTO(workout, wes));
    }
} 