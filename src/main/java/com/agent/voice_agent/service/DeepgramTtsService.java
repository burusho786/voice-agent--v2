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

        long start = System.currentTimeMillis();

        byte[] audio = deepgramRestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/speak")
                        .queryParam("model", properties.model())
                        .queryParam("encoding", properties.encoding())
                        .queryParam("bit_rate", properties.bitRate())
                        .queryParam("speed", properties.speed())
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.valueOf("audio/mpeg"))
                .body(request)
                .retrieve()
                .body(byte[].class);

        long elapsed = System.currentTimeMillis() - start;

        if (audio == null || audio.length == 0) {
            throw new IllegalStateException(
                    "Deepgram returned empty audio"
            );
        }

        double approxSeconds = properties.bitRate() > 0
                ? (audio.length * 8.0) / properties.bitRate()
                : 0.0;

        log.info(
                "TTS generated | {} ms | {} bytes | ~{} s audio | model={} | speed={} | bitRate={}",
                elapsed,
                audio.length,
                String.format("%.2f", approxSeconds),
                properties.model(),
                properties.speed(),
                properties.bitRate()
        );

        return audio;
    }
}