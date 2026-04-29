package com.sit.campusbackend.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the campus backend.
 *
 * Provides:
 *  1. A shared BCryptPasswordEncoder bean (inject wherever passwords are hashed/checked).
 *  2. A permissive SecurityFilterChain that disables CSRF and allows all requests
 *     while JWT is not yet fully implemented.
 *
 * TODO: When JWT is ready, add the JwtAuthFilter before
 *       UsernamePasswordAuthenticationFilter and lock down endpoints.
 */
@Configuration
public class SecurityConfig {

    /**
     * Shared BCryptPasswordEncoder — always inject this bean, never
     * instantiate {@code new BCryptPasswordEncoder()} inline.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Permit all requests and disable CSRF so the REST API is reachable
     * without credentials while JWT auth is pending.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
