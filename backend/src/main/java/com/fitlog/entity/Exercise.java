package com.fitlog.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

// JPA Entity representing an exercise
@Entity
@Table(name = "exercises")
public class Exercise {
    // Primary key, auto-generated
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // Whether this exercise is public
    @Column(nullable = false)
    private boolean isPublic;

    // Muscle groups targeted by this exercise
    @Column(length = 100)
    private String muscleGroups;

    // Name of the exercise (must be unique and not null)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // Whether this exercise is active (for soft deletion)
    @Column(nullable = false)
    private boolean isActive = true;

    // Optional notes for the exercise
    @Column(length = 500)
    private String notes;

    // When the exercise was created
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // When the exercise was last updated
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Reference to the user who created this exercise (ownership)
    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

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

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public String getMuscleGroups() { return muscleGroups; }
    public void setMuscleGroups(String muscleGroups) { this.muscleGroups = muscleGroups; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean isActive) { this.isActive = isActive; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
} 