package com.agent.voice_agent.service;

import com.agent.voice_agent.dto.DocumentChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChunkingService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    public List<DocumentChunk> chunk(String documentText) {

        if (documentText == null || documentText.isBlank()) {
            return List.of();
        }

        String normalizedText =
                documentText
                        .replace("\r\n", "\n")
                        .replace("\r", "\n")
                        .trim();

        List<DocumentChunk> chunks = new ArrayList<>();

        int start = 0;
        int chunkNumber = 1;

        while (start < normalizedText.length()) {

            int end = Math.min(
                    start + CHUNK_SIZE,
                    normalizedText.length()
            );

            String chunkText =
                    normalizedText
                            .substring(start, end)
                            .trim();

            if (!chunkText.isBlank()) {

                Map<String, String> metadata =
                        new HashMap<>();

                metadata.put(
                        "chunkNumber",
                        String.valueOf(chunkNumber)
                );

                chunks.add(
                        new DocumentChunk(
                                "chunk-" + chunkNumber,
                                chunkText,
                                metadata
                        )
                );

                chunkNumber++;
            }

            if (end >= normalizedText.length()) {
                break;
            }

            start = end - CHUNK_OVERLAP;
        }

        return chunks;
    }
}