package com.fitlog.controller;

import com.fitlog.entity.WorkoutExercise;
import com.fitlog.entity.Workout;
import com.fitlog.entity.Exercise;
import com.fitlog.repository.WorkoutExerciseRepository;
import com.fitlog.repository.WorkoutRepository;
import com.fitlog.repository.ExerciseRepository;
import com.fitlog.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import java.util.*;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

// Controller for workout exercise-related endpoints
@Tag(name = "WorkoutExercise", description = "Operations related to workout exercises.")
@RestController
@RequestMapping("/workout_exercises")
public class WorkoutExerciseController {
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final WorkoutRepository workoutRepository;
    private final ExerciseRepository exerciseRepository;
    private final JwtUtil jwtUtil;

    @Autowired
    public WorkoutExerciseController(
            WorkoutExerciseRepository workoutExerciseRepository,
            WorkoutRepository workoutRepository,
            ExerciseRepository exerciseRepository,
            JwtUtil jwtUtil) {
        this.workoutExerciseRepository = workoutExerciseRepository;
        this.workoutRepository = workoutRepository;
        this.exerciseRepository = exerciseRepository;
        this.jwtUtil = jwtUtil;
    }

    // Simple DTO for user info (for extracting userId from JWT)
    private static class UserInfo {
        UUID userId;
        UserInfo(UUID userId) { this.userId = userId; }
    }

    // DTO for creating/updating workout exercises
    public static class WorkoutExerciseRequest {
        public UUID workoutId;
        public UUID exerciseId;
        public int position;
        public String sets;
        public String notes;
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

    /**
     * Create a new workout exercise. Only the owner of the workout can create.
     */
    @Operation(summary = "Create workout exercise", description = "Create a new workout exercise for a workout you own.")
    @PostMapping
    public ResponseEntity<?> createWorkoutExercise(
            @RequestBody WorkoutExerciseRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        var userInfo = userInfoOpt.get();
        // Check workout ownership
        Optional<Workout> workoutOpt = workoutRepository.findById(request.workoutId);
        if (workoutOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Workout not found."));
        }
        Workout workout = workoutOpt.get();
        if (!workout.getUser().getId().equals(userInfo.userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can only add exercises to your own workouts."));
        }
        // Check exercise exists
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(request.exerciseId);
        if (exerciseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Exercise not found."));
        }
        Exercise exercise = exerciseOpt.get();
        // Create and save
        WorkoutExercise workoutExercise = new WorkoutExercise();
        workoutExercise.setWorkout(workout);
        workoutExercise.setExercise(exercise);
        workoutExercise.setPosition(request.position);
        workoutExercise.setSets(request.sets);
        workoutExercise.setNotes(request.notes);
        workoutExerciseRepository.save(workoutExercise);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", workoutExercise.getId(),
                "workoutId", workout.getId(),
                "exerciseId", exercise.getId(),
                "position", workoutExercise.getPosition(),
                "sets", workoutExercise.getSets(),
                "notes", workoutExercise.getNotes(),
                "createdAt", workoutExercise.getCreatedAt(),
                "updatedAt", workoutExercise.getUpdatedAt()
        ));
    }

    /**
     * Update a workout exercise. Only the owner of the associated workout can update.
     */
    @Operation(summary = "Update workout exercise", description = "Update a workout exercise. Only the owner of the workout can update.")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkoutExercise(
            @PathVariable UUID id,
            @RequestBody WorkoutExerciseRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        var userInfo = userInfoOpt.get();
        Optional<WorkoutExercise> weOpt = workoutExerciseRepository.findById(id);
        if (weOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Workout exercise not found."));
        }
        WorkoutExercise workoutExercise = weOpt.get();
        // Check workout ownership
        if (!workoutExercise.getWorkout().getUser().getId().equals(userInfo.userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can only update exercises in your own workouts."));
        }
        // Optionally update fields
        if (request.exerciseId != null) {
            Optional<Exercise> exerciseOpt = exerciseRepository.findById(request.exerciseId);
            if (exerciseOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Exercise not found."));
            }
            workoutExercise.setExercise(exerciseOpt.get());
        }
        if (request.position != 0) {
            workoutExercise.setPosition(request.position);
        }
        if (request.sets != null) {
            workoutExercise.setSets(request.sets);
        }
        if (request.notes != null) {
            workoutExercise.setNotes(request.notes);
        }
        workoutExerciseRepository.save(workoutExercise);
        return ResponseEntity.ok(Map.of(
                "id", workoutExercise.getId(),
                "workoutId", workoutExercise.getWorkout().getId(),
                "exerciseId", workoutExercise.getExercise().getId(),
                "position", workoutExercise.getPosition(),
                "sets", workoutExercise.getSets(),
                "notes", workoutExercise.getNotes(),
                "createdAt", workoutExercise.getCreatedAt(),
                "updatedAt", workoutExercise.getUpdatedAt()
        ));
    }

    /**
     * Delete a workout exercise (hard delete). Only the owner of the workout can delete.
     */
    @Operation(summary = "Delete workout exercise", description = "Hard delete a workout exercise. Only the owner of the workout can delete.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkoutExercise(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        var userInfo = userInfoOpt.get();
        Optional<WorkoutExercise> weOpt = workoutExerciseRepository.findById(id);
        if (weOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Workout exercise not found."));
        }
        WorkoutExercise workoutExercise = weOpt.get();
        // Check workout ownership
        if (!workoutExercise.getWorkout().getUser().getId().equals(userInfo.userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can only delete exercises in your own workouts."));
        }
        workoutExerciseRepository.delete(workoutExercise);
        return ResponseEntity.ok(Map.of("message", "Workout exercise deleted."));
    }

    /**
     * Get all workout exercises for a workout. Only the owner can view.
     */
    @Operation(summary = "Get all workout exercises for a workout", description = "Get all workout exercises for a workout you own.")
    @GetMapping("/by_workout/{workoutId}")
    public ResponseEntity<?> getWorkoutExercisesByWorkout(
            @PathVariable UUID workoutId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        var userInfo = userInfoOpt.get();
        Optional<Workout> workoutOpt = workoutRepository.findById(workoutId);
        if (workoutOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Workout not found."));
        }
        Workout workout = workoutOpt.get();
        if (!workout.getUser().getId().equals(userInfo.userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can only view exercises for your own workouts."));
        }
        List<WorkoutExercise> exercises = workoutExerciseRepository.findByWorkoutId(workoutId);
        return ResponseEntity.ok(exercises);
    }

    /**
     * Get a single workout exercise by ID. Only the owner of the workout can view.
     */
    @Operation(summary = "Get single workout exercise", description = "Get a single workout exercise by ID. Only the owner of the workout can view.")
    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkoutExerciseById(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        var userInfo = userInfoOpt.get();
        Optional<WorkoutExercise> weOpt = workoutExerciseRepository.findById(id);
        if (weOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Workout exercise not found."));
        }
        WorkoutExercise workoutExercise = weOpt.get();
        if (!workoutExercise.getWorkout().getUser().getId().equals(userInfo.userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You are not allowed to access this workout exercise."));
        }
        return ResponseEntity.ok(workoutExercise);
    }
} 