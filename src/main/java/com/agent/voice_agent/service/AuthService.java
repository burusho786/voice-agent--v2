package com.agent.voice_agent.service;

import com.agent.voice_agent.dto.LoginRequest;
import com.agent.voice_agent.dto.LoginResponse;
import com.agent.voice_agent.dto.RegisterRequest;
import com.agent.voice_agent.dto.RegisterResponse;
import com.agent.voice_agent.exception.DuplicateResourceException;
import com.agent.voice_agent.security.JwtUtil;
import com.agent.voice_agent.security.model.Role;
import com.agent.voice_agent.security.model.User;
import com.agent.voice_agent.security.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public RegisterResponse register(RegisterRequest request) {

        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "Email is already registered"
            );
        }

        String passwordHash =
                passwordEncoder.encode(request.password());

        Instant now = Instant.now();

        User user = new User(
                email,
                passwordHash,
                Set.of(Role.USER),
                true,
                now,
                now
        );

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                "User registered successfully"
        );
    }

    public LoginResponse login(LoginRequest request) {

        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or password"
                        )
                );

        if (!user.isEnabled()) {
            throw new IllegalStateException(
                    "User account is disabled"
            );
        }

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        String token = jwtUtil.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                jwtUtil.getExpirationTime()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}