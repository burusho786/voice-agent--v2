package com.agent.voice_agent.controller;

import com.agent.voice_agent.dto.TranscriptionResponse;
import com.agent.voice_agent.service.DeepgramService;
import com.agent.voice_agent.component.AudioFileValidator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/speech-to-text")
public class SpeechToTextController {

    private final DeepgramService deepgramService;
    private final AudioFileValidator audioFileValidator;

    public SpeechToTextController(
            DeepgramService deepgramService,
            AudioFileValidator audioFileValidator
    ) {
        this.deepgramService = deepgramService;
        this.audioFileValidator = audioFileValidator;
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TranscriptionResponse> transcribe(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        audioFileValidator.validate(file);

        TranscriptionResponse response =
                deepgramService.transcribe(
                        file.getBytes(),
                        file.getContentType()
                );

        return ResponseEntity.ok(response);
    }
}