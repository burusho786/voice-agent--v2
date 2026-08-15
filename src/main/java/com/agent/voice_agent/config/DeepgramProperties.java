package com.agent.voice_agent.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deepgram")
public record DeepgramProperties(
        String apiKey,
        String baseUrl,
        String model,
        boolean smartFormat
) {
}