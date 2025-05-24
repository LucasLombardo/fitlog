package com.fitlog.controller;

import com.fitlog.entity.User;
import com.fitlog.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.fitlog.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;

// Controller for user-related endpoints
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;

    // Inject the UserRepository and JwtUtil via constructor
    @Autowired
    public UserController(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public List<Map<String, ?>> getUsers() {
        // Fetch all users from the database
        List<User> users = userRepository.findAll();
        // Map each user to a response DTO (excluding password)
        return users.stream().map(user -> Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "createdAt", user.getCreatedAt(),
            "updatedAt", user.getUpdatedAt(),
            "role", user.getRole()
        )).collect(Collectors.toList());
    }

    // DTO for user creation request
    public static class CreateUserRequest {
        public String email;
        public String password;
        public String role; // Optional, default to USER
    }

    /**
     * Endpoint to create a new user.
     * In the future, add email verification and send a verification email here.
     * For password reset, store a reset token and send email when needed.
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        // Basic validation
        if (request.email == null || request.email.isBlank() || request.password == null || request.password.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Email and password are required."));
        }
        // Check if email is already used
        if (userRepository.findByEmail(request.email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already in use."));
        }
        // Hash the password
        String hashedPassword = passwordEncoder.encode(request.password);
        // Create and save the user
        User user = new User();
        user.setEmail(request.email);
        user.setPassword(hashedPassword);
        user.setRole(request.role != null ? request.role : "USER");
        // createdAt/updatedAt are set automatically
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid data or duplicate email."));
        }
        // Return non-sensitive info only
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "createdAt", user.getCreatedAt(),
            "updatedAt", user.getUpdatedAt(),
            "role", user.getRole()
        ));
    }

    /**
     * Endpoint for user login.
     * Returns a JWT on successful login.
     * Never reveal if email or password was incorrect for security.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody CreateUserRequest request) {
        if (request.email == null || request.email.isBlank() || request.password == null || request.password.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Email and password are required."));
        }
        var userOpt = userRepository.findByEmail(request.email);
        if (userOpt.isEmpty()) {
            // Do not reveal if email or password is wrong
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials."));
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(request.password, user.getPassword())) {
            // Do not reveal if email or password is wrong
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials."));
        }
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        // Return token and user info (non-sensitive)
        return ResponseEntity.ok(Map.of(
            "token", token,
            "user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "createdAt", user.getCreatedAt(),
                "updatedAt", user.getUpdatedAt(),
                "role", user.getRole()
            )
        ));
    }

    /**
     * Endpoint to delete a user by ID.
     * In a real app, restrict this to admins or the user themselves.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found."));
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
    }
} 