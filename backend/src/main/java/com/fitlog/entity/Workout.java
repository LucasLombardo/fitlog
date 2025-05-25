package com.fitlog.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// JPA Entity representing a workout
@Entity
@Table(name = "workouts")
public class Workout {
    // Primary key, auto-generated
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // Reference to the user who owns this workout
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Date of the workout (user input)
    @Column(nullable = false)
    private LocalDate date;

    // Optional notes for the workout
    @Column(length = 500)
    private String notes;

    // When the workout was created
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // When the workout was last updated
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Set timestamps automatically
    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
} 