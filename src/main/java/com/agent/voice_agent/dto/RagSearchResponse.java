package com.agent.voice_agent.dto;

import java.util.List;

public record RagSearchResponse(
        String query,
        List<RagSearchResult> results
) {
}