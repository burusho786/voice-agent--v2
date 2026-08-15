package com.agent.voice_agent.Config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeepgramConfig {

    @Value("${deepgram.api-key}")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

}
