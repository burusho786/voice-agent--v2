package com.agent.voice_agent.service;

import com.agent.voice_agent.config.DeepgramTtsProperties;
import com.agent.voice_agent.dto.TtsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class DeepgramTtsService {

    private static final Logger log =
            LoggerFactory.getLogger(DeepgramTtsService.class);

    private final RestClient deepgramRestClient;
    private final DeepgramTtsProperties properties;

    public DeepgramTtsService(
            RestClient deepgramRestClient,
            DeepgramTtsProperties properties
    ) {
        this.deepgramRestClient = deepgramRestClient;
        this.properties = properties;
    }

    public byte[] synthesize(String text) {

        TtsRequest request = new TtsRequest(text);

        byte[] audio = deepgramRestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/speak")
                        .queryParam("model", properties.model())
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.valueOf("audio/mpeg"))
                .body(request)
                .retrieve()
                .body(byte[].class);

        if (audio == null || audio.length == 0) {
            throw new IllegalStateException(
                    "Deepgram returned empty audio"
            );
        }

        log.info(
                "TTS generated successfully. Audio size: {} bytes",
                audio.length
        );

        return audio;
    }
}