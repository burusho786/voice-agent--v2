package com.agent.voice_agent.service;

import com.agent.voice_agent.dto.LlmResponse;
import org.springframework.stereotype.Service;

@Service
public class LlmToSpeechService {

    private final OllamaLlmService ollamaLlmService;
    private final DeepgramTtsService deepgramTtsService;

    public LlmToSpeechService(
            OllamaLlmService ollamaLlmService,
            DeepgramTtsService deepgramTtsService
    ) {
        this.ollamaLlmService = ollamaLlmService;
        this.deepgramTtsService = deepgramTtsService;
    }

    public byte[] generateSpeech(String prompt) {

        // 1. Send prompt to Gemma
        LlmResponse llmResponse =
                ollamaLlmService.generateResponse(prompt);

        // 2. Extract only the actual generated text
        String responseText =
                llmResponse.response();

        // 3. Send Gemma's text to Deepgram TTS
        return deepgramTtsService.synthesize(responseText);
    }
}