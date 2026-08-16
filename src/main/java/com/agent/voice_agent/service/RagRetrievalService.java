package com.agent.voice_agent.service;

import com.agent.voice_agent.dto.VectorSearchResult;
import com.agent.voice_agent.vector.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagRetrievalService {

    private static final int DEFAULT_TOP_K = 3;

    private final OllamaEmbeddingService embeddingService;
    private final VectorStore vectorStore;

    public RagRetrievalService(
            OllamaEmbeddingService embeddingService,
            VectorStore vectorStore
    ) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    public List<VectorSearchResult> retrieve(
            String query
    ) {

        return retrieve(
                query,
                DEFAULT_TOP_K
        );
    }

    public List<VectorSearchResult> retrieve(
            String query,
            int topK
    ) {

        if (query == null ||
                query.isBlank()) {

            throw new IllegalArgumentException(
                    "Query cannot be null or blank"
            );
        }

        if (topK <= 0) {
            throw new IllegalArgumentException(
                    "topK must be greater than zero"
            );
        }

        if (vectorStore.size() == 0) {
            throw new IllegalStateException(
                    "Vector store is empty. " +
                            "Ingest a document first."
            );
        }

        List<Double> queryEmbedding =
                embeddingService.generateEmbedding(
                        query
                );

        return vectorStore.search(
                queryEmbedding,
                topK
        );
    }
}