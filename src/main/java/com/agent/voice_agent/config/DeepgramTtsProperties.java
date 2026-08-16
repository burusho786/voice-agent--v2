package com.agent.voice_agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deepgram.tts")
public record DeepgramTtsProperties(
        String model
) {
}