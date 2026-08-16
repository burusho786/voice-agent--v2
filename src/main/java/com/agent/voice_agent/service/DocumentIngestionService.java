package com.agent.voice_agent.service;

import com.agent.voice_agent.dto.DocumentChunk;
import com.agent.voice_agent.dto.EmbeddedChunk;
import com.agent.voice_agent.vector.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentIngestionService {

    private final ChunkingService chunkingService;
    private final OllamaEmbeddingService embeddingService;
    private final VectorStore vectorStore;

    public DocumentIngestionService(
            ChunkingService chunkingService,
            OllamaEmbeddingService embeddingService,
            VectorStore vectorStore
    ) {
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    public List<EmbeddedChunk> ingest(String documentText) {

        if (documentText == null || documentText.isBlank()) {
            throw new IllegalArgumentException(
                    "Document text cannot be null or blank"
            );
        }

        List<DocumentChunk> chunks =
                chunkingService.chunk(documentText);

        List<EmbeddedChunk> embeddedChunks =
                chunks.stream()
                        .map(this::createEmbeddedChunk)
                        .toList();

        /*
         * Store the generated embeddings in HNSW.
         */
        vectorStore.addAll(embeddedChunks);

        return embeddedChunks;
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