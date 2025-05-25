package com.fitlog.controller;

import com.fitlog.entity.Exercise;
import com.fitlog.repository.ExerciseRepository;
import com.fitlog.repository.UserRepository;
import com.fitlog.entity.User;
import com.fitlog.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;

// Controller for exercise-related endpoints
@Tag(name = "Exercise", description = "Operations related to exercises.")
@RestController
@RequestMapping("/exercises")
public class ExerciseController {
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Autowired
    public ExerciseController(ExerciseRepository exerciseRepository, UserRepository userRepository, JwtUtil jwtUtil) {
        this.exerciseRepository = exerciseRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
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
            String role = claims.get("role", String.class);
            return Optional.of(new UserInfo(userId, role));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Simple DTO for user info
    private static class UserInfo {
        UUID userId;
        String role;
        UserInfo(UUID userId, String role) { this.userId = userId; this.role = role; }
    }

    // DTO for creating/updating exercises
    public static class ExerciseRequest {
        public String name;
        public boolean isPublic;
        public String muscleGroups;
        public String notes;
    }

    /**
     * Create a new exercise. Any authenticated user can create. Only admins can set isPublic=true.
     */
    @Operation(summary = "Create exercise", description = "Create a new exercise. Only admins can set isPublic=true.")
    @PostMapping
    public ResponseEntity<?> createExercise(
            @RequestBody ExerciseRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        UserInfo userInfo = userInfoOpt.get();
        // Only admins can set isPublic=true
        if (request.isPublic && !"ADMIN".equals(userInfo.role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only admins can create public exercises."));
        }
        // Check for unique name
        if (exerciseRepository.findByName(request.name).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Exercise name already exists."));
        }
        // Set createdBy to the current user
        Optional<User> userOpt = userRepository.findById(userInfo.userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found."));
        }
        User user = userOpt.get();
        Exercise exercise = new Exercise();
        exercise.setName(request.name);
        exercise.setPublic(request.isPublic);
        exercise.setMuscleGroups(request.muscleGroups);
        exercise.setNotes(request.notes);
        exercise.setActive(true);
        exercise.setCreatedBy(user);
        exerciseRepository.save(exercise);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", exercise.getId(),
                "name", exercise.getName(),
                "isPublic", exercise.isPublic(),
                "muscleGroups", exercise.getMuscleGroups(),
                "notes", exercise.getNotes(),
                "isActive", exercise.isActive()
        ));
    }

    /**
     * Update an exercise. Admins can update any, users only their own. Only admins can set isPublic=true.
     */
    @Operation(summary = "Update exercise", description = "Update an exercise. Only admins can set isPublic=true. Users can only update their own.")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateExercise(
            @PathVariable UUID id,
            @RequestBody ExerciseRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        UserInfo userInfo = userInfoOpt.get();
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        if (exerciseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Exercise not found."));
        }
        Exercise exercise = exerciseOpt.get();
        // Ownership check: if not admin, only allow if user owns the exercise
        if (!"ADMIN".equals(userInfo.role) && !exercise.getCreatedBy().getId().equals(userInfo.userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can only update your own exercises."));
        }
        // Only admins can set isPublic=true
        if (request.isPublic && !"ADMIN".equals(userInfo.role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only admins can set isPublic=true."));
        }
        // Update fields
        exercise.setName(request.name);
        exercise.setPublic(request.isPublic);
        exercise.setMuscleGroups(request.muscleGroups);
        exercise.setNotes(request.notes);
        exerciseRepository.save(exercise);
        return ResponseEntity.ok(Map.of(
                "id", exercise.getId(),
                "name", exercise.getName(),
                "isPublic", exercise.isPublic(),
                "muscleGroups", exercise.getMuscleGroups(),
                "notes", exercise.getNotes(),
                "isActive", exercise.isActive()
        ));
    }

    /**
     * Get all exercises. Users get public + their own, admins get all.
     */
    @Operation(summary = "Get all exercises", description = "Users get public + their own, admins get all.")
    @GetMapping
    public ResponseEntity<?> getExercises(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        UserInfo userInfo = userInfoOpt.get();
        List<Exercise> exercises;
        if ("ADMIN".equals(userInfo.role)) {
            exercises = exerciseRepository.findAll();
        } else {
            exercises = exerciseRepository.findAll().stream()
                    .filter(e -> e.isPublic() || e.getCreatedBy().getId().equals(userInfo.userId))
                    .collect(Collectors.toList());
        }
        // Return only active exercises
        exercises = exercises.stream().filter(Exercise::isActive).collect(Collectors.toList());
        return ResponseEntity.ok(exercises);
    }

    /**
     * Get a single exercise. Users can get public or their own, admins can get any.
     */
    @Operation(summary = "Get single exercise", description = "Users can get public or their own, admins can get any.")
    @GetMapping("/{id}")
    public ResponseEntity<?> getExerciseById(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        UserInfo userInfo = userInfoOpt.get();
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        if (exerciseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Exercise not found."));
        }
        Exercise exercise = exerciseOpt.get();
        if (!exercise.isActive()) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", "Exercise is deleted."));
        }
        if ("ADMIN".equals(userInfo.role) || exercise.isPublic() || exercise.getCreatedBy().getId().equals(userInfo.userId)) {
            return ResponseEntity.ok(exercise);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You are not allowed to access this exercise."));
        }
    }

    /**
     * Soft delete an exercise. Admins can delete any, users only their own.
     */
    @Operation(summary = "Delete exercise (soft)", description = "Soft delete an exercise. Admins can delete any, users only their own.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExercise(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest servletRequest) {
        var userInfoOpt = getUserInfo(authHeader, servletRequest);
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        UserInfo userInfo = userInfoOpt.get();
        Optional<Exercise> exerciseOpt = exerciseRepository.findById(id);
        if (exerciseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Exercise not found."));
        }
        Exercise exercise = exerciseOpt.get();
        // Ownership check: if not admin, only allow if user owns the exercise
        if (!"ADMIN".equals(userInfo.role) && !exercise.getCreatedBy().getId().equals(userInfo.userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can only delete your own exercises."));
        }
        exercise.setActive(false); // Soft delete
        exerciseRepository.save(exercise);
        return ResponseEntity.ok(Map.of("message", "Exercise deleted (soft)."));
    }
} 