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
import com.fitlog.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.core.env.Environment;

// Controller for user-related endpoints
@Tag(name = "User", description = "Operations related to user management, registration, login, and deletion.")
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;
    private final Environment env; // Inject Spring Environment to check active profiles

    // Inject the UserRepository, JwtUtil, and Environment via constructor
    @Autowired
    public UserController(UserRepository userRepository, JwtUtil jwtUtil, Environment env) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.env = env;
    }

    @Operation(
        summary = "Get all users",
        description = "Returns a list of all users (excluding passwords). Only accessible to users with the ADMIN role.",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of users returned successfully."),
            @ApiResponse(responseCode = "403", description = "Forbidden: Only accessible to ADMINs.")
        }
    )
    @PreAuthorize("hasRole('ADMIN')") // Only allow ADMINs
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
        // Removed role field for security: all new users are USER by default
    }

    /**
     * Endpoint to create a new user.
     * In the future, add email verification and send a verification email here.
     * For password reset, store a reset token and send email when needed.
     */
    @Operation(
        summary = "Create a new user",
        description = "Registers a new user with email and password. All new users are assigned the USER role by default. Returns the created user (excluding password).",
        responses = {
            @ApiResponse(responseCode = "201", description = "User created successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input or duplicate email.")
        }
    )
    @PostMapping
    public ResponseEntity<?> createUser(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User registration data (email and password only)",
            required = true,
            content = @Content(schema = @Schema(implementation = CreateUserRequest.class))
        )
        @RequestBody CreateUserRequest request) {
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
        user.setRole("USER"); // Always set to USER for self-registration
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
    @Operation(
        summary = "User login",
        description = "Authenticates a user and returns a JWT token on success.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Login successful, JWT returned."),
            @ApiResponse(responseCode = "400", description = "Missing email or password."),
            @ApiResponse(responseCode = "401", description = "Invalid credentials.")
        }
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User login data",
            required = true,
            content = @Content(schema = @Schema(implementation = CreateUserRequest.class))
        )
        @RequestBody CreateUserRequest request) {
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

        // Determine if we are in dev or test profile
        String[] activeProfiles = env.getActiveProfiles();
        boolean isDevOrTest = false;
        for (String profile : activeProfiles) {
            if (profile.equals("dev") || profile.equals("test")) {
                isDevOrTest = true;
                break;
            }
        }

        // Set JWT as HttpOnly, Secure cookie
        org.springframework.http.ResponseCookie.ResponseCookieBuilder cookieBuilder = org.springframework.http.ResponseCookie.from("jwt", token)
            .httpOnly(true) // Prevent JS access
            .secure(true)   // Only send over HTTPS
            .path("/")
            .sameSite("Strict")
            .maxAge(24 * 60 * 60); // 1 day
        // Only set domain in production (not dev/test)
        if (!isDevOrTest) {
            cookieBuilder.domain(".fitlogapp.com"); // Allow cookie for all subdomains
        }
        org.springframework.http.ResponseCookie cookie = cookieBuilder.build();

        // Return user info only (no token in body)
        return ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString())
            .body(Map.of(
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
     * Endpoint for user logout.
     * Removes the JWT cookie from the browser by setting it with maxAge=0 and HttpOnly.
     * This effectively logs the user out on the client side.
     *
     * Security: Always use HttpOnly and Secure flags for cookies.
     */
    @Operation(
        summary = "User logout",
        description = "Logs out the user by removing the JWT cookie.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Logout successful, JWT cookie removed.")
        }
    )
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Determine if we are in dev or test profile
        String[] activeProfiles = env.getActiveProfiles();
        boolean isDevOrTest = false;
        for (String profile : activeProfiles) {
            if (profile.equals("dev") || profile.equals("test")) {
                isDevOrTest = true;
                break;
            }
        }
        // To log out, set the JWT cookie with maxAge=0 (expires immediately)
        org.springframework.http.ResponseCookie.ResponseCookieBuilder cookieBuilder = org.springframework.http.ResponseCookie.from("jwt", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite("Strict")
            .maxAge(0); // Expire immediately
        // Only set domain in production (not dev/test)
        if (!isDevOrTest) {
            cookieBuilder.domain(".fitlogapp.com");
        }
        org.springframework.http.ResponseCookie cookie = cookieBuilder.build();

        // Return a response with the expired cookie and a message
        return ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString())
            .body(Map.of("message", "Logout successful. JWT cookie removed."));
    }

    /**
     * Endpoint to delete a user by ID.
     * In a real app, restrict this to admins or the user themselves.
     */
    @Operation(
        summary = "Delete a user",
        description = "Deletes a user by their ID. Only accessible to users with the ADMIN role.",
        parameters = {
            @Parameter(name = "id", description = "ID of the user to delete", required = true)
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully."),
            @ApiResponse(responseCode = "403", description = "Forbidden: Only accessible to ADMINs."),
            @ApiResponse(responseCode = "404", description = "User not found.")
        }
    )
    @PreAuthorize("hasRole('ADMIN')") // Only allow ADMINs
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found."));
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
    }

    /**
     * Endpoint to get a single user by ID.
     * Only accessible to admins or the user themselves.
     */
    @Operation(
        summary = "Get a single user",
        description = "Returns a single user's info (excluding password). Only accessible to admins or the user themselves.",
        parameters = {
            @Parameter(name = "id", description = "ID of the user to fetch", required = true)
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "User returned successfully."),
            @ApiResponse(responseCode = "403", description = "Forbidden: Not allowed to access this user."),
            @ApiResponse(responseCode = "404", description = "User not found.")
        }
    )
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable UUID id,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authHeader,
            jakarta.servlet.http.HttpServletRequest request) {
        // Check for Bearer token or cookie
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        if (token == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Missing or invalid Authorization header."));
        }
        // Parse JWT to extract userId and role
        UUID requesterId;
        String requesterRole;
        try {
            var claims = jwtUtil.validateToken(token);
            requesterId = UUID.fromString(claims.get("userId", String.class));
            requesterRole = claims.get("role", String.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired token."));
        }
        // Only allow if admin or requesting their own user
        if (!("ADMIN".equals(requesterRole) || (requesterId != null && requesterId.equals(id)))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You are not allowed to access this user."));
        }
        // Fetch user from DB
        var userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found."));
        }
        User user = userOpt.get();
        // Return non-sensitive info only
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "createdAt", user.getCreatedAt(),
            "updatedAt", user.getUpdatedAt(),
            "role", user.getRole()
        ));
    }

    /**
     * Endpoint for a user to delete their own account.
     * Only the authenticated user can delete themselves (no admin required).
     *
     * Security: Only deletes the user whose ID is in the validated JWT.
     */
    @Operation(
        summary = "Delete own account",
        description = "Allows the authenticated user to delete their own account. Requires a valid JWT.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Account deleted successfully."),
            @ApiResponse(responseCode = "401", description = "Invalid or missing token."),
            @ApiResponse(responseCode = "404", description = "User not found.")
        }
    )
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteOwnAccount(
            @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authHeader,
            jakarta.servlet.http.HttpServletRequest request) {
        // Check for Bearer token or cookie
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing or invalid Authorization header."));
        }
        UUID userId;
        try {
            // Validate the JWT and extract the userId claim
            var claims = jwtUtil.validateToken(token);
            userId = UUID.fromString(claims.get("userId", String.class));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired token."));
        }
        // Check if the user exists
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found."));
        }
        // Delete the user
        userRepository.deleteById(userId);
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully."));
    }
} 