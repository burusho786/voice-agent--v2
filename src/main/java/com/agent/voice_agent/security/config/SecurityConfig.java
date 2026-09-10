package com.agent.voice_agent.security.config;

import com.agent.voice_agent.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Public authentication endpoints
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/static/**",
                                "/css/**",
                                "/js/**"
                        ).permitAll()

                        .requestMatchers("/api/final/**")
                        .hasAnyRole("USER", "ADMIN")

                       // .requestMatchers("/api/final/**").permitAll()

                        // Protected voice/RAG endpoints
                        .requestMatchers(
                                "/api/voice/**",
                                "/api/rag/query"
                        ).hasAnyRole("USER", "ADMIN")

                        // Document endpoints
                        .requestMatchers(
                                "/api/documents/ingest",
                        //"/api/voice/**",
                                "/api/documents/**"
                        ).hasAnyRole("ADMIN", "USER")

                        // Everything else
                        .anyRequest().permitAll()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}