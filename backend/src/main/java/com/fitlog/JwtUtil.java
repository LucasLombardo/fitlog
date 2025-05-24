package com.fitlog;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Date;
import java.security.Key;
import org.springframework.stereotype.Component;

// Utility class for generating and validating JWT tokens
@Component
public class JwtUtil {
    // Use a strong secret key (in production, load from environment variable or config)
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor("ReplaceThisWithASecretKeyOfAtLeast32Bytes!123456".getBytes());
    // Token validity: 24 hours
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000;

    // Generate a JWT token for a user
    public String generateToken(Long userId, String email, String role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("email", email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate and parse a JWT token
    public Claims validateToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
} 