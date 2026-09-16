package com.agent.voice_agent.service;

import com.agent.voice_agent.config.OllamaProperties;
import com.agent.voice_agent.dto.LlmResponse;
import com.agent.voice_agent.dto.OllamaGenerateRequest;
import com.agent.voice_agent.dto.OllamaGenerateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OllamaLlmService {

    private static final Logger log =
            LoggerFactory.getLogger(OllamaLlmService.class);

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

        OllamaGenerateRequest request = new OllamaGenerateRequest(
                properties.model(),
                prompt,
                false,
                properties.keepAlive(),
                new OllamaGenerateRequest.Options(
                        properties.numPredict(),
                        properties.temperature()
                )
        );

        long wallStart = System.currentTimeMillis();

        OllamaGenerateResponse response = ollamaRestClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OllamaGenerateResponse.class);

        long wallMs = System.currentTimeMillis() - wallStart;

        if (response == null || response.response() == null) {
            throw new IllegalStateException(
                    "Ollama returned an empty response"
            );
        }

        logOllamaTimings(response, wallMs);

        return new LlmResponse(response.response());
    }

    private void logOllamaTimings(OllamaGenerateResponse r, long wallMs) {

        long loadMs       = nsToMs(r.loadDuration());
        long promptEvalMs = nsToMs(r.promptEvalDuration());
        long evalMs       = nsToMs(r.evalDuration());
        long totalMs      = nsToMs(r.totalDuration());

        double tokensPerSec = (evalMs > 0 && r.evalCount() != null)
                ? (r.evalCount() * 1000.0 / evalMs)
                : 0.0;

        log.info(
                "Ollama timings | wall={} ms | total={} ms | load={} ms | " +
                        "prompt_eval={} ms ({} tok) | eval={} ms ({} tok, {} tok/s)",
                wallMs,
                totalMs,
                loadMs,
                promptEvalMs,
                r.promptEvalCount(),
                evalMs,
                r.evalCount(),
                String.format("%.1f", tokensPerSec)
        );
    }

    private static long nsToMs(Long ns) {
        return ns == null ? 0L : ns / 1_000_000L;
    }
}