package com.fitlog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Controller for health check endpoint
@Tag(name = "Health", description = "Health check endpoint for service status.")
@RestController
public class HealthController {
    @Operation(
        summary = "Health check",
        description = "Returns a simple status message to indicate the service is running.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Service is healthy.")
        }
    )
    @GetMapping("/health")
    public Map<String, String> health() {
        // Return a simple status message
        return Map.of("status", "ok");
    }
} 