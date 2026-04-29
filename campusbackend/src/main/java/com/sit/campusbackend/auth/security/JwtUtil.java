package com.sit.campusbackend.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT utility — skeleton ready for full implementation.
 *
 * To complete JWT support:
 *  1. Add to pom.xml:
 *       io.jsonwebtoken:jjwt-api:0.11.5
 *       io.jsonwebtoken:jjwt-impl:0.11.5  (runtime)
 *       io.jsonwebtoken:jjwt-jackson:0.11.5 (runtime)
 *  2. Uncomment the Jwts.* calls below.
 *  3. Inject JwtUtil into AuthController and DepartmentController.
 *  4. Implement JwtAuthFilter (extends OncePerRequestFilter) and
 *     register it in SecurityConfig before UsernamePasswordAuthenticationFilter.
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
     * @return placeholder string (replace with real token once jjwt is added)
     *
     * TODO:
     * <pre>
     * return Jwts.builder()
     *     .setSubject(email)
     *     .claim("role", role)
     *     .setIssuedAt(new Date())
     *     .setExpiration(new Date(System.currentTimeMillis() + expiryMs))
     *     .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
     *     .compact();
     * </pre>
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
