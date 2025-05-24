package com.fitlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Main entry point for the Spring Boot application
// Swagger UI (OpenAPI docs) is enabled only in dev/test environments for security.
// This is controlled via application-dev.properties and application-test.properties.
// See springdoc.swagger-ui.enabled property.
@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
} 