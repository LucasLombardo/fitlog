package com.fitlog.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

// JPA Entity representing a user in the system
@Entity
@Table(name = "users")
public class User {
    // Primary key, auto-generated
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // User's email address (should be unique)
    @Column(nullable = false, unique = true)
    private String email;

    // Hashed password (never store plain text passwords!)
    @Column(nullable = false)
    private String password;

    // When the user was created
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // When the user was last updated
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // User's role (e.g., 'USER', 'ADMIN')
    @Column(nullable = false)
    private String role;

    // Email verification code (null if already verified)
    // This code is sent to the user's email and must be entered to verify their account.
    @Column(length = 10)
    private String emailVerificationCode;

    // When the verification code expires (1 hour after sending)
    private LocalDateTime emailVerificationExpiry;

    // Whether the email is verified (true if user has completed verification)
    @Column(nullable = false)
    private boolean emailVerified = false;

    // Set timestamps automatically
    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters (required for JPA)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmailVerificationCode() { return emailVerificationCode; }
    public void setEmailVerificationCode(String emailVerificationCode) { this.emailVerificationCode = emailVerificationCode; }

    public LocalDateTime getEmailVerificationExpiry() { return emailVerificationExpiry; }
    public void setEmailVerificationExpiry(LocalDateTime emailVerificationExpiry) { this.emailVerificationExpiry = emailVerificationExpiry; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
} 