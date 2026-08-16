package com.agent.voice_agent.dto;

import java.util.Map;

public record DocumentChunk(
        String id,
        String content,
        Map<String, String> metadata
) {
}