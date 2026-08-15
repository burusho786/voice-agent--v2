package com.agent.voice_agent.controller;


import com.agent.voice_agent.dto.TranscriptionResponse;
import com.agent.voice_agent.service.DeepgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/speech-to-text")
public class speech_to_text {


    private final DeepgramService deepgramService;

    public speech_to_text(DeepgramService deepgramService) {
        this.deepgramService = deepgramService;
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TranscriptionResponse> transcribe(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        TranscriptionResponse response =
                deepgramService.transcribe(
                        file.getBytes(),
                        file.getContentType()
                );

        return ResponseEntity.ok(response);
    }
}

