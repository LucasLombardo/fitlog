package com.fitlog.repository;

import com.fitlog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

// Repository interface for User entity
// Provides CRUD operations and query methods
public interface UserRepository extends JpaRepository<User, UUID> {
    // Find a user by email (optional, useful for login)
    Optional<User> findByEmail(String email);
} 