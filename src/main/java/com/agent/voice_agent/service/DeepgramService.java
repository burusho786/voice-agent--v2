package com.agent.voice_agent.service;

import com.agent.voice_agent.Config.DeepgramConfig;
import com.agent.voice_agent.dto.DeepgramResponse;
import com.agent.voice_agent.dto.TranscriptionResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DeepgramService {

    private static final String DEEPGRAM_URL =
            "https://api.deepgram.com/v1/listen"
                    + "?model=nova-3"
                    + "&smart_format=true";

    private final DeepgramConfig deepgramConfig;
    private final RestTemplate restTemplate;

    public DeepgramService(DeepgramConfig deepgramConfig) {
        this.deepgramConfig = deepgramConfig;
        this.restTemplate = new RestTemplate();
    }

    public TranscriptionResponse transcribe(
            byte[] audioBytes,
            String contentType
    ) {

        HttpHeaders headers = new HttpHeaders();

        headers.set(
                "Authorization",
                "Token " + deepgramConfig.getApiKey()
        );

        headers.setContentType(
                MediaType.parseMediaType(contentType)
        );

        HttpEntity<byte[]> request =
                new HttpEntity<>(audioBytes, headers);

        ResponseEntity<DeepgramResponse> response =
                restTemplate.exchange(
                        DEEPGRAM_URL,
                        HttpMethod.POST,
                        request,
                        DeepgramResponse.class
                );

        String transcript = extractTranscript(response.getBody());

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