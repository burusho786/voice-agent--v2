package com.agent.voice_agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({DeepgramProperties.class,DeepgramTtsProperties.class})

public class DeepgramClientConfig {

    @Bean
    public RestClient deepgramRestClient(
            DeepgramProperties properties
    ) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(
                        "Authorization",
                        "Token " + properties.apiKey()
                )
                .build();
    }
}