package com.ngo.donation_management.config;

// config/SecurityConfig.java

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication
        .configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method
        .configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web
        .builders.HttpSecurity;
import org.springframework.security.config.annotation.web
        .configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication
        .UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return bcrypt.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                if (encodedPassword == null || encodedPassword.isBlank()) {
                    return false;
                }

                if (encodedPassword.startsWith("$2a$")
                        || encodedPassword.startsWith("$2b$")
                        || encodedPassword.startsWith("$2y$")) {
                    return bcrypt.matches(rawPassword, encodedPassword);
                }

                return rawPassword != null && encodedPassword.equals(rawPassword.toString());
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ngos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/campaigns/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/urgent-needs/**").permitAll()

                        // Donor endpoints
                        .requestMatchers("/api/donations/**")
                        .hasAnyRole("DONOR", "ADMIN", "NGO_ADMIN")
                        .requestMatchers("/api/donation-requests/**")
                        .hasAnyRole("DONOR", "ADMIN", "NGO_ADMIN")
                        .requestMatchers("/api/pickups/**")
                        .hasAnyRole("DONOR", "ADMIN", "NGO_ADMIN",
                                "VOLUNTEER")
                        .requestMatchers("/api/payments/**")
                        .hasAnyRole("DONOR", "ADMIN", "NGO_ADMIN")
                        .requestMatchers("/api/receipts/**")
                        .hasAnyRole("DONOR", "ADMIN", "NGO_ADMIN")
                        .requestMatchers("/api/donation-items/**")
                        .hasAnyRole("DONOR", "ADMIN", "NGO_ADMIN")

                        // Volunteer endpoints
                        .requestMatchers("/api/tasks/**")
                        .hasAnyRole("VOLUNTEER", "ADMIN", "NGO_ADMIN")
                        .requestMatchers("/api/volunteers/**")
                        .hasAnyRole("VOLUNTEER", "ADMIN", "NGO_ADMIN")

                        // Volunteer self-service task endpoints (NEW)
                        .requestMatchers("/api/volunteer-tasks/available-pickups")
                        .authenticated()
                        .requestMatchers("/api/volunteer-tasks/**")
                        .authenticated()

                        // Location / Map endpoints (NEW)
                        .requestMatchers(HttpMethod.GET, "/api/locations/pending")
                        .hasAnyRole("VOLUNTEER", "ADMIN", "NGO_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/locations/maps-key")
                        .hasAnyRole("VOLUNTEER", "ADMIN", "NGO_ADMIN", "DONOR")
                        .requestMatchers("/api/locations/**")
                        .hasAnyRole("DONOR", "VOLUNTEER", "ADMIN", "NGO_ADMIN")

                        // Notification endpoints
                        .requestMatchers(HttpMethod.GET,
                                "/api/notifications/me",
                                "/api/notifications/me/unread-count")
                        .hasAnyRole("DONOR", "VOLUNTEER", "ADMIN", "NGO_ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/notifications/me/read-all",
                                "/api/notifications/*/read")
                        .hasAnyRole("DONOR", "VOLUNTEER", "ADMIN", "NGO_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/notifications/test-email")
                        .hasRole("ADMIN")

                        // Admin endpoints
                        .requestMatchers(HttpMethod.GET, "/api/reports/public-summary")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/me")
                        .hasAnyRole("DONOR", "VOLUNTEER", "ADMIN", "NGO_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/me")
                        .hasAnyRole("DONOR", "VOLUNTEER", "ADMIN", "NGO_ADMIN")
                        .requestMatchers("/api/users/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/reports/**")
                        .hasAnyRole("ADMIN", "NGO_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/ngos/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/ngos/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/ngos/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/campaigns/**")
                        .hasAnyRole("ADMIN", "NGO_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/campaigns/**")
                        .hasAnyRole("ADMIN", "NGO_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/campaigns/**")
                        .hasAnyRole("ADMIN", "NGO_ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/urgent-needs/**")
                        .hasAnyRole("ADMIN", "NGO_ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/urgent-needs/**")
                        .hasAnyRole("ADMIN", "NGO_ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/urgent-needs/**")
                        .hasAnyRole("ADMIN", "NGO_ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
