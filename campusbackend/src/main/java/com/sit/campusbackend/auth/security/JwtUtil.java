package com.sit.campusbackend.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT utility — fully implemented.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiry-ms}")
    private long expiryMs;

    /**
     * Generate a signed JWT for the given email and role.
     *
     * @param email subject — student/admin/dept email
     * @param role  "STUDENT", "ADMIN", or "DEPARTMENT"
     * @return signed JWT string
     */
    public String generateToken(String email, String role) {
        return io.jsonwebtoken.Jwts.builder()
            .setSubject(email)
            .claim("role", role)
            .setIssuedAt(new java.util.Date())
            .setExpiration(new java.util.Date(System.currentTimeMillis() + expiryMs))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes()), io.jsonwebtoken.SignatureAlgorithm.HS256)
            .compact();
    }

    public String validateAndGetEmail(String token) {
        return io.jsonwebtoken.Jwts.parserBuilder()
            .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes()))
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    public String extractRole(String token) {
        return io.jsonwebtoken.Jwts.parserBuilder()
            .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes()))
            .build()
            .parseClaimsJws(token)
            .getBody()
            .get("role", String.class);
    }
}
