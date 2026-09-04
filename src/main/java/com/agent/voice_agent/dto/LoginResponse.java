package com.agent.voice_agent.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}