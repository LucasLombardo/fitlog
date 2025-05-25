package com.fitlog.repository;

import com.fitlog.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

// Repository for Workout entity
@Repository
public interface WorkoutRepository extends JpaRepository<Workout, UUID> {
    // Find all workouts by user ID
    List<Workout> findByUserId(UUID userId);
    // Add more custom queries as needed
} 