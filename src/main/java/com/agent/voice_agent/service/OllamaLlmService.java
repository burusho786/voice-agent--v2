package com.agent.voice_agent.service;

import com.agent.voice_agent.config.OllamaProperties;
import com.agent.voice_agent.dto.LlmResponse;
import com.agent.voice_agent.dto.OllamaGenerateRequest;
import com.agent.voice_agent.dto.OllamaGenerateResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OllamaLlmService {

    private final RestClient ollamaRestClient;
    private final OllamaProperties properties;

    public OllamaLlmService(
            RestClient ollamaRestClient,
            OllamaProperties properties
    ) {
        this.ollamaRestClient = ollamaRestClient;
        this.properties = properties;
    }

    public LlmResponse generateResponse(String prompt) {

        OllamaGenerateRequest request =
                new OllamaGenerateRequest(
                        properties.model(),
                        prompt,
                        false
                );

        OllamaGenerateResponse response =
                ollamaRestClient.post()
                        .uri("/api/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(OllamaGenerateResponse.class);

        if (response == null || response.response() == null) {
            throw new IllegalStateException(
                    "Ollama returned an empty response"
            );
        }

        return new LlmResponse(response.response());
    }
}