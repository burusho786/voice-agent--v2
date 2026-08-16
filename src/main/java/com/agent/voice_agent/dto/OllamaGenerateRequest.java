package com.agent.voice_agent.dto;

public record OllamaGenerateRequest(
        String model,
        String prompt,
        boolean stream
) {
}