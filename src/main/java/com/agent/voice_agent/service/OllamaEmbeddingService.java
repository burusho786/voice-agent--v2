package com.agent.voice_agent.service;

import com.agent.voice_agent.config.OllamaEmbeddingProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class OllamaEmbeddingService {

    private final RestClient ollamaEmbeddingRestClient;
    private final OllamaEmbeddingProperties properties;

    public OllamaEmbeddingService(
            RestClient ollamaEmbeddingRestClient,
            OllamaEmbeddingProperties properties
    ) {
        this.ollamaEmbeddingRestClient =
                ollamaEmbeddingRestClient;

        this.properties = properties;
    }

    public List<Double> generateEmbedding(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Text cannot be null or blank"
            );
        }

        OllamaEmbeddingRequest request =
                new OllamaEmbeddingRequest(
                        properties.model(),
                        text
                );

        OllamaEmbeddingResponse response =
                ollamaEmbeddingRestClient
                        .post()
                        .uri("/api/embeddings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(OllamaEmbeddingResponse.class);

        if (response == null || response.embedding() == null) {
            throw new IllegalStateException(
                    "Ollama returned an empty embedding"
            );
        }

        return response.embedding();
    }

    private record OllamaEmbeddingRequest(
            String model,
            String prompt
    ) {
    }

    private record OllamaEmbeddingResponse(
            List<Double> embedding
    ) {
    }
}