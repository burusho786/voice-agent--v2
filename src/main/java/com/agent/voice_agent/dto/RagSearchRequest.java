package com.agent.voice_agent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RagSearchRequest(

        @NotBlank(message = "Query cannot be blank")
        String query,

        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 10, message = "topK cannot exceed 10")
        Integer topK
) {
}