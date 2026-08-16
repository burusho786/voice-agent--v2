package com.agent.voice_agent.dto;

import jakarta.validation.constraints.NotBlank;

public record TtsRequest(

        @NotBlank(message = "Text must not be blank")
        String text

) {
}