package com.integration_service.common.security;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final InternalApiSecurityFilter internalApiSecurityFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // We permit all at the Spring Security level because our custom 
                        // InternalApiSecurityFilter handles the actual authorization logic 
                        // (validating X-Internal-Secret) before reaching the controllers.
                        // Public endpoints bypass this check inside the filter itself.
                        .anyRequest().permitAll()
                )
                // Add our custom security filter
                .addFilterBefore(internalApiSecurityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

