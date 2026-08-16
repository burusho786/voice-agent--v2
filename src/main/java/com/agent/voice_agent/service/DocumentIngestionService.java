package com.agent.voice_agent.service;

import com.agent.voice_agent.dto.DocumentChunk;
import com.agent.voice_agent.dto.DocumentIngestionResponse;
import com.agent.voice_agent.dto.EmbeddedChunk;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class DocumentIngestionService {

    private final ChunkingService chunkingService;
    private final OllamaEmbeddingService embeddingService;

    public DocumentIngestionService(
            ChunkingService chunkingService,
            OllamaEmbeddingService embeddingService
    ) {
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
    }

    public DocumentIngestionResponse ingest(
            MultipartFile file
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Document file cannot be empty"
            );
        }

        String documentText =
                new String(
                        file.getBytes(),
                        StandardCharsets.UTF_8
                );

        if (documentText.isBlank()) {
            throw new IllegalArgumentException(
                    "Document does not contain any text"
            );
        }

        List<DocumentChunk> chunks =
                chunkingService.chunk(documentText);

        List<EmbeddedChunk> embeddedChunks =
                chunks.stream()
                        .map(this::createEmbeddedChunk)
                        .toList();

        return new DocumentIngestionResponse(
                file.getOriginalFilename(),
                embeddedChunks.size(),
                embeddedChunks
        );
    }

    private EmbeddedChunk createEmbeddedChunk(
            DocumentChunk chunk
    ) {

        List<Double> embedding =
                embeddingService.generateEmbedding(
                        chunk.content()
                );

        return new EmbeddedChunk(
                chunk.id(),
                chunk.content(),
                embedding,
                chunk.metadata()
        );
    }
}