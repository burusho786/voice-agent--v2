package com.agent.voice_agent.dto;

import java.util.List;

public record DocumentIngestionResponse(
        String filename,
        int chunkCount,
        List<EmbeddedChunk> chunks
) {
}