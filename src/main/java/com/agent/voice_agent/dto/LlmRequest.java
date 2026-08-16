package com.agent.voice_agent.dto;

import jakarta.validation.constraints.NotBlank;

public record LlmRequest(
        @NotBlank(message = "Prompt must not be blank")
        String prompt
) {
}