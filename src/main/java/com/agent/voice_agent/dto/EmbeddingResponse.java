package com.agent.voice_agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EmbeddingResponse(
        @JsonProperty("embedding")
        List<Float> embedding
) {
}