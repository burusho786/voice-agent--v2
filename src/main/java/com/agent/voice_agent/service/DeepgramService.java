package com.agent.voice_agent.service;

import com.agent.voice_agent.config.DeepgramProperties;
import com.agent.voice_agent.dto.DeepgramResponse;
import com.agent.voice_agent.dto.TranscriptionResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class DeepgramService {

    private final RestClient deepgramRestClient;
    private final DeepgramProperties properties;

    public DeepgramService(
            RestClient deepgramRestClient,
            DeepgramProperties properties
    ) {
        this.deepgramRestClient = deepgramRestClient;
        this.properties = properties;
    }

    public TranscriptionResponse transcribe(
            byte[] audioBytes,
            String contentType
    ) {


        DeepgramResponse response = deepgramRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/listen")
                        .queryParam("model", properties.model())
                        .queryParam(
                                "smart_format",
                                properties.smartFormat()
                        )
                        .build())
                .contentType(MediaType.parseMediaType(contentType))
                .body(audioBytes)
                .retrieve()
                .body(DeepgramResponse.class);

        String transcript = extractTranscript(response);

        return new TranscriptionResponse(transcript);
    }

    private String extractTranscript(DeepgramResponse response) {

        if (response == null
                || response.results() == null
                || response.results().channels() == null
                || response.results().channels().isEmpty()
                || response.results().channels().get(0).alternatives() == null
                || response.results().channels().get(0).alternatives().isEmpty()) {

            return "";
        }

        String transcript = response.results()
                .channels()
                .get(0)
                .alternatives()
                .get(0)
                .transcript();

        return transcript == null ? "" : transcript;
    }
}