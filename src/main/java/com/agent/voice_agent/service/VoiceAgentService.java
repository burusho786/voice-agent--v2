package com.agent.voice_agent.service;

import com.agent.voice_agent.component.RagPromptBuilder;
import com.agent.voice_agent.dto.LlmResponse;
import com.agent.voice_agent.dto.TranscriptionResponse;
import com.agent.voice_agent.dto.VectorSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoiceAgentService {

    private static final Logger log =
            LoggerFactory.getLogger(VoiceAgentService.class);

    private final DeepgramService deepgramService;
    private final RagRetrievalService ragRetrievalService;
    private final RagPromptBuilder ragPromptBuilder;
    private final OllamaLlmService ollamaLlmService;
    private final DeepgramTtsService deepgramTtsService;

    public VoiceAgentService(
            DeepgramService deepgramService,
            RagRetrievalService ragRetrievalService,
            RagPromptBuilder ragPromptBuilder,
            OllamaLlmService ollamaLlmService,
            DeepgramTtsService deepgramTtsService
    ) {
        this.deepgramService = deepgramService;
        this.ragRetrievalService = ragRetrievalService;
        this.ragPromptBuilder = ragPromptBuilder;
        this.ollamaLlmService = ollamaLlmService;
        this.deepgramTtsService = deepgramTtsService;
    }

    /**
     * Backwards-compatible entry point. Returns only synthesized audio.
     * Serves POST /api/final/process.
     */
    public byte[] process(byte[] audioBytes, String contentType) {
        return runPipeline(audioBytes, contentType).audio();
    }

    /**
     * Full pipeline result including transcript and LLM text.
     * Serves POST /api/final/process-with-metadata.
     */
    public PipelineResult processWithMetadata(
            byte[] audioBytes,
            String contentType
    ) {
        return runPipeline(audioBytes, contentType);
    }

    private PipelineResult runPipeline(byte[] audioBytes, String contentType) {

        long totalStart = System.currentTimeMillis();

        log.info(
                "VOICE PIPELINE STARTED | Audio size: {} bytes | Content-Type: {}",
                audioBytes.length,
                contentType
        );

        /* STEP 1 — Audio -> Deepgram STT -> Transcript */
        long sttStart = System.currentTimeMillis();

        TranscriptionResponse transcription =
                deepgramService.transcribe(audioBytes, contentType);

        long sttTime = System.currentTimeMillis() - sttStart;

        String transcript = transcription.transcript();

        if (transcript == null || transcript.isBlank()) {
            throw new IllegalArgumentException(
                    "No speech could be detected in the audio"
            );
        }

        log.info("STT completed in {} ms | Transcript: {}", sttTime, transcript);

        /* STEP 2 — Transcript -> RAG Retrieval */
        long ragStart = System.currentTimeMillis();

        List<VectorSearchResult> results =
                ragRetrievalService.retrieve(transcript, 1);

        long ragTime = System.currentTimeMillis() - ragStart;

        if (results == null || results.isEmpty()) {
            throw new IllegalStateException(
                    "No relevant company information was found"
            );
        }

        log.info(
                "RAG retrieval completed in {} ms | Results: {}",
                ragTime,
                results.size()
        );

        /* STEP 3 — Question + Context -> Prompt */
        String prompt = ragPromptBuilder.build(transcript, results);

        log.info(
                "RAG prompt built successfully | Context chunks: {}",
                results.size()
        );

        /* STEP 4 — Prompt -> Gemma */
        long llmStart = System.currentTimeMillis();

        LlmResponse llmResponse = ollamaLlmService.generateResponse(prompt);

        long llmTime = System.currentTimeMillis() - llmStart;

        String responseText = llmResponse.response();

        if (responseText == null || responseText.isBlank()) {
            throw new IllegalStateException("LLM returned an empty response");
        }

        log.info(
                "LLM completed in {} ms | Response length: {} characters",
                llmTime,
                responseText.length()
        );

        /* STEP 5 — Response -> Deepgram TTS -> Audio */
        long ttsStart = System.currentTimeMillis();

        byte[] audioResponse = deepgramTtsService.synthesize(responseText);

        long ttsTime = System.currentTimeMillis() - ttsStart;

        log.info(
                "TTS completed in {} ms | Audio size: {} bytes",
                ttsTime,
                audioResponse.length
        );

        long totalTime = System.currentTimeMillis() - totalStart;

        log.info(
                "VOICE PIPELINE COMPLETED | STT: {} ms | RAG: {} ms | LLM: {} ms | TTS: {} ms | TOTAL: {} ms",
                sttTime, ragTime, llmTime, ttsTime, totalTime
        );

        return new PipelineResult(transcript, responseText, audioResponse);
    }

    /**
     * Internal carrier of the full pipeline output.
     * Not serialized to HTTP clients directly.
     */
    public record PipelineResult(
            String transcript,
            String responseText,
            byte[] audio
    ) {
    }
}