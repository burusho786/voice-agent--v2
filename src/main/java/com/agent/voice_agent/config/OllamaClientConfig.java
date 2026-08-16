package com.agent.voice_agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class OllamaClientConfig {

    @Bean
    public RestClient ollamaRestClient(
            OllamaProperties properties
    ) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }
}