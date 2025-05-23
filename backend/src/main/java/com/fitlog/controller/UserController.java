package com.fitlog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

// Controller for user-related endpoints
@RestController
@RequestMapping("/users")
public class UserController {
    @GetMapping
    public List<Map<String, String>> getUsers() {
        // Placeholder: return a static list of users
        return List.of(
            Map.of("id", "1", "username", "alice"),
            Map.of("id", "2", "username", "bob")
        );
    }
} 