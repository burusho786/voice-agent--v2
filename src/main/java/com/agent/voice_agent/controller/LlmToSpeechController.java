package com.agent.voice_agent.controller;

import com.agent.voice_agent.dto.LlmRequest;
import com.agent.voice_agent.service.LlmToSpeechService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/llm")
public class LlmToSpeechController {

    private final LlmToSpeechService llmToSpeechService;

    public LlmToSpeechController(
            LlmToSpeechService llmToSpeechService
    ) {
        this.llmToSpeechService = llmToSpeechService;
    }

    @PostMapping(
            value = "/speak",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = "audio/mpeg"
    )
    public ResponseEntity<byte[]> speak(
            @RequestBody LlmRequest request
    ) {

        byte[] audio =
                llmToSpeechService.generateSpeech(
                        request.prompt()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"llm-response.mp3\""
                )
                .contentType(MediaType.valueOf("audio/mpeg"))
                .contentLength(audio.length)
                .body(audio);
    }
}