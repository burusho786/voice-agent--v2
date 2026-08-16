package com.agent.voice_agent.service;

import com.agent.voice_agent.dto.LlmResponse;
import com.agent.voice_agent.dto.TranscriptionResponse;
import org.springframework.stereotype.Service;

@Service
public class VoiceAgentService {

    private final DeepgramService deepgramService;
    private final OllamaLlmService ollamaLlmService;
    private final DeepgramTtsService deepgramTtsService;

    public VoiceAgentService(
            DeepgramService deepgramService,
            OllamaLlmService ollamaLlmService,
            DeepgramTtsService deepgramTtsService
    ) {
        this.deepgramService = deepgramService;
        this.ollamaLlmService = ollamaLlmService;
        this.deepgramTtsService = deepgramTtsService;
    }

    public byte[] process(
            byte[] audioBytes,
            String contentType
    ) {

        /*
         * Step 1:
         * Audio -> Deepgram Speech-to-Text
         */
        TranscriptionResponse transcription =
                deepgramService.transcribe(
                        audioBytes,
                        contentType
                );

        String transcript = transcription.transcript();

        if (transcript == null || transcript.isBlank()) {
            throw new IllegalArgumentException(
                    "No speech could be detected in the audio"
            );
        }

        /*
         * Step 2:
         * Transcript -> Ollama Gemma
         */
        LlmResponse llmResponse =
                ollamaLlmService.generateResponse(transcript);

        /*
         * Extract only the actual generated response.
         *
         * Do not use llmResponse.toString().
         */
        String responseText = llmResponse.response();

        if (responseText == null || responseText.isBlank()) {
            throw new IllegalStateException(
                    "LLM returned an empty response"
            );
        }

        /*
         * Step 3:
         * Gemma response -> Deepgram Text-to-Speech
         */
        return deepgramTtsService.synthesize(responseText);
    }
}