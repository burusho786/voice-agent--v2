package com.agent.voice_agent.controller;

import com.agent.voice_agent.dto.TtsRequest;
import com.agent.voice_agent.service.DeepgramTtsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/text-to-speech")
public class TextToSpeechController {

    private final DeepgramTtsService deepgramTtsService;

    public TextToSpeechController(DeepgramTtsService deepgramTtsService) {
        this.deepgramTtsService = deepgramTtsService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = "audio/mpeg"
    )
    public ResponseEntity<byte[]> synthesize(
            @Valid @RequestBody TtsRequest request
    ) {

        byte[] audio = deepgramTtsService.synthesize(request.text());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"response.mp3\""
                )
                .contentType(MediaType.valueOf("audio/mpeg"))
                .contentLength(audio.length)
                .body(audio);

    }
}