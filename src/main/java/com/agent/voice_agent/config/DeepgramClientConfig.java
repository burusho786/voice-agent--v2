package com.agent.voice_agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties({DeepgramProperties.class, DeepgramTtsProperties.class})
public class DeepgramClientConfig {

    @Bean
    public RestClient deepgramRestClient(DeepgramProperties properties) {

        // TODO (follow-up): move these two Durations into DeepgramProperties
        // once that file is in scope. They are inlined here to avoid touching
        // a class we do not currently have visibility into.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(
                        "Authorization",
                        "Token " + properties.apiKey()
                )
                .requestFactory(requestFactory)
                .build();
    }
}