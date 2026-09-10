package com.agent.voice_agent.controller;

import com.agent.voice_agent.component.AudioFileValidator;
import com.agent.voice_agent.dto.VoiceAgentResponse;
import com.agent.voice_agent.service.VoiceAgentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/api/final")
public class VoiceAgentController {

    private static final String AUDIO_MIME_TYPE = "audio/mpeg";

    private final VoiceAgentService voiceAgentService;
    private final AudioFileValidator audioFileValidator;

    public VoiceAgentController(
            VoiceAgentService voiceAgentService,
            AudioFileValidator audioFileValidator
    ) {
        this.voiceAgentService = voiceAgentService;
        this.audioFileValidator = audioFileValidator;
    }

    /**
     * Existing endpoint. Returns raw MP3. Contract unchanged.
     */
    @PostMapping(
            value = "/process",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = AUDIO_MIME_TYPE
    )
    public ResponseEntity<byte[]> process(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        audioFileValidator.validate(file);

        byte[] responseAudio = voiceAgentService.process(
                file.getBytes(),
                file.getContentType()
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"voice-response.mp3\""
                )
                .contentType(MediaType.valueOf(AUDIO_MIME_TYPE))
                .contentLength(responseAudio.length)
                .body(responseAudio);
    }

    /**
     * New endpoint for the chat UI.
     * Returns transcript, LLM text, and base64 audio in one JSON payload.
     */
    @PostMapping(
            value = "/process-with-metadata",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<VoiceAgentResponse> processWithMetadata(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        audioFileValidator.validate(file);

        VoiceAgentService.PipelineResult result =
                voiceAgentService.processWithMetadata(
                        file.getBytes(),
                        file.getContentType()
                );

        VoiceAgentResponse body = new VoiceAgentResponse(
                result.transcript(),
                result.responseText(),
                Base64.getEncoder().encodeToString(result.audio()),
                AUDIO_MIME_TYPE
        );

        return ResponseEntity.ok(body);
    }
}