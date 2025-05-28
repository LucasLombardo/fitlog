package com.fitlog.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

// JPA Entity representing an exercise within a workout
@Entity
@Table(name = "workout_exercises")
public class WorkoutExercise {
    // Primary key, auto-generated
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // Reference to the workout (cascade on delete)
    @ManyToOne(optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    // Reference to the exercise
    @ManyToOne(optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    // Position/order of the exercise in the workout
    @Column(nullable = false)
    private int position;

    // Sets data stored as JSON string (for flexibility)
    @Column(columnDefinition = "TEXT")
    private String sets;

    // Optional notes for this workout exercise
    @Column(length = 500)
    private String notes;

    // When the workout exercise was created
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // When the workout exercise was last updated
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

    public Workout getWorkout() { return workout; }
    public void setWorkout(Workout workout) { this.workout = workout; }

    public Exercise getExercise() { return exercise; }
    public void setExercise(Exercise exercise) { this.exercise = exercise; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String getSets() { return sets; }
    public void setSets(String sets) { this.sets = sets; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
} 