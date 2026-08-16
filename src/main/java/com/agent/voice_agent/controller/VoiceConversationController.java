package com.agent.voice_agent.controller;

import com.agent.voice_agent.dto.VoiceConversationResponse;
import com.agent.voice_agent.service.VoiceConversationService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/voice")
public class VoiceConversationController {

    private final VoiceConversationService voiceConversationService;

    public VoiceConversationController(
            VoiceConversationService voiceConversationService
    ) {
        this.voiceConversationService = voiceConversationService;
    }

    @PostMapping(
            value = "/process",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<VoiceConversationResponse> process(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        VoiceConversationResponse response =
                voiceConversationService.process(
                        file.getBytes(),
                        file.getContentType()
                );

        return ResponseEntity.ok(response);
    }
}