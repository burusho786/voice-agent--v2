package com.agent.voice_agent.dto;

public record VectorSearchResult(
        EmbeddedChunk chunk,
        double score
) {
}