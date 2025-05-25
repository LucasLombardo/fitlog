package com.fitlog.repository;

import com.fitlog.entity.WorkoutExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

// Repository for WorkoutExercise entity
@Repository
public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, UUID> {
    // Find all workout exercises by workout ID
    List<WorkoutExercise> findByWorkoutId(UUID workoutId);
    // Add more custom queries as needed
} 