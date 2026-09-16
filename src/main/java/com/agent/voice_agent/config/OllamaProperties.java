package com.agent.voice_agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "ollama")
public record OllamaProperties(
        String baseUrl,
        String model,

        // How long Ollama keeps the model resident after the last request.
        // "-1" = never unload, "30m" = 30 minutes, "0" = unload immediately.
        @DefaultValue("30m") String keepAlive,

        // Hard ceiling on generated tokens. Bounds worst-case latency.
        @DefaultValue("200") Integer numPredict,

        // Optional; if null, Ollama's per-model default applies.
        Double temperature,

        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("120s") Duration readTimeout
) {
}