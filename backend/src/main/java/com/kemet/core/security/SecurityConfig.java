package com.kemet.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Core Service validates Auth0-issued JWTs (RS256, verified against AUTH0_ISSUER_URI's
 * JWKS endpoint via Spring's oauth2ResourceServer auto-config) on every request. It
 * never handles a password itself — that's Auth0's job, matching ADR-003 in the
 * architecture spec. All endpoints require a valid token; there is no anonymous access
 * in this prototype (no public-content tier yet — REQ-004 is out of scope for v0).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/actuator/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        return http.build();
    }
}
