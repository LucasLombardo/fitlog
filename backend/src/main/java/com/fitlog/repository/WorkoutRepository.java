package com.fitlog.repository;

import com.fitlog.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.util.Optional;

// Repository for Workout entity
@Repository
public interface WorkoutRepository extends JpaRepository<Workout, UUID> {
    // Find all workouts by user ID
    List<Workout> findByUserId(UUID userId);
    // Find a workout by user and date
    Optional<Workout> findByUserIdAndDate(UUID userId, LocalDate date);
    // Add more custom queries as needed
} 