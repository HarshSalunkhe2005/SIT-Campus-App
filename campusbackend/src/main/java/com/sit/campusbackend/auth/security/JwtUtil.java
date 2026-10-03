package com.sit.campusbackend.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Issues and verifies HS256 JWTs. The signing key is built once at startup. */
@Component
public class JwtUtil {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expiryMs;

    public JwtUtil(@Value("${jwt.secret}") String secret, @Value("${jwt.expiry-ms}") long expiryMs) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_BYTES + " characters long.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiryMs = expiryMs;
    }

    /** @param role "STUDENT", "ADMIN" or "DEPARTMENT" */
    public String generateToken(String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiryMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Verifies signature and expiry and returns the claims; throws {@link JwtException} when invalid. */
    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}
