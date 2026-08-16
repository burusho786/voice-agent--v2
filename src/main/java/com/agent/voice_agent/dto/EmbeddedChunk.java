package com.agent.voice_agent.dto;

import java.util.List;
import java.util.Map;

public record EmbeddedChunk(
        String id,
        String content,
        List<Double> embedding,
        Map<String, String> metadata
) {
}