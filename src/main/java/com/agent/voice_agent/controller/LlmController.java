package com.agent.voice_agent.controller;

import com.agent.voice_agent.dto.LlmRequest;
import com.agent.voice_agent.dto.LlmResponse;
import com.agent.voice_agent.service.OllamaLlmService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final OllamaLlmService ollamaLlmService;

    public LlmController(OllamaLlmService ollamaLlmService) {
        this.ollamaLlmService = ollamaLlmService;
    }

    @PostMapping(
            value = "/generate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<LlmResponse> generate(
            @Valid @RequestBody LlmRequest request
    ) {

        LlmResponse response =
                ollamaLlmService.generateResponse(request.prompt());

        return ResponseEntity.ok(response);
    }
}