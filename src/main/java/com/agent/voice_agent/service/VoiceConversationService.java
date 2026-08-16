package com.agent.voice_agent.service;

import com.agent.voice_agent.dto.TranscriptionResponse;
import com.agent.voice_agent.dto.VoiceConversationResponse;
import org.springframework.stereotype.Service;

@Service
public class VoiceConversationService {

    private final DeepgramService deepgramService;
    private final OllamaLlmService ollamaLlmService;

    public VoiceConversationService(
            DeepgramService deepgramService,
            OllamaLlmService ollamaLlmService
    ) {
        this.deepgramService = deepgramService;
        this.ollamaLlmService = ollamaLlmService;
    }

    public VoiceConversationResponse process(
            byte[] audioBytes,
            String contentType
    ) {

        // Step 1: Audio → Transcript
        TranscriptionResponse transcription =
                deepgramService.transcribe(
                        audioBytes,
                        contentType
                );

        String transcript = transcription.transcript();

        // Step 2: Transcript → LLM
        String response =
                String.valueOf(ollamaLlmService.generateResponse(
                        transcript
                ));

        return new VoiceConversationResponse(
                transcript,
                response
        );
    }
}