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
        // TODO: replace with real jjwt implementation (see Javadoc above)
        return "JWT_PLACEHOLDER_FOR_" + role + "_" + email;
    }

    /**
     * Validate a JWT and return the embedded subject (email).
     *
     * @param token JWT from the Authorization header
     * @return email embedded in the token
     *
     * TODO:
     * <pre>
     * return Jwts.parserBuilder()
     *     .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
     *     .build()
     *     .parseClaimsJws(token)
     *     .getBody()
     *     .getSubject();
     * </pre>
     */
    public String validateAndGetEmail(String token) {
        throw new UnsupportedOperationException(
            "JWT validation not yet implemented — add jjwt dependency first.");
    }

    /**
     * Extract the role claim from a validated token.
     * TODO: implement after adding jjwt.
     */
    public String extractRole(String token) {
        throw new UnsupportedOperationException(
            "JWT role extraction not yet implemented — add jjwt dependency first.");
    }
}
