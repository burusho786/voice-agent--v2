package com.agent.voice_agent.dto;

import java.util.Map;

public record RagSearchResult(
        String id,
        String content,
        double score,
        Map<String, String> metadata
) {
}