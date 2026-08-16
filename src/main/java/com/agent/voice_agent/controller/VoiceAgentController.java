package com.agent.voice_agent.controller;

import com.agent.voice_agent.service.VoiceAgentService;
import com.agent.voice_agent.component.AudioFileValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/final")
public class VoiceAgentController {

    private final VoiceAgentService voiceAgentService;
    private final AudioFileValidator audioFileValidator;

    public VoiceAgentController(
            VoiceAgentService voiceAgentService,
            AudioFileValidator audioFileValidator
    ) {
        this.voiceAgentService = voiceAgentService;
        this.audioFileValidator = audioFileValidator;
    }

    @PostMapping(
            value = "/process",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "audio/mpeg"
    )
    public ResponseEntity<byte[]> process(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        /*
         * Validate the uploaded audio file before
         * sending anything to Deepgram.
         */
        audioFileValidator.validate(file);

        /*
         * Execute:
         *
         * Audio
         *   -> Deepgram STT
         *   -> Gemma
         *   -> Deepgram TTS
         *   -> Audio
         */
        byte[] responseAudio =
                voiceAgentService.process(
                        file.getBytes(),
                        file.getContentType()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"voice-response.mp3\""
                )
                .contentType(MediaType.valueOf("audio/mpeg"))
                .contentLength(responseAudio.length)
                .body(responseAudio);
    }
}