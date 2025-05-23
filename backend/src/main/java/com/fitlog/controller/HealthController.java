package com.fitlog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

// Controller for health check endpoint
@RestController
public class HealthController {
    @GetMapping("/health")
    public Map<String, String> health() {
        // Return a simple status message
        return Map.of("status", "ok");
    }
} 