package com.agent.voice_agent.controller;

import com.agent.voice_agent.dto.DocumentIngestionResponse;
import com.agent.voice_agent.dto.EmbeddedChunk;
import com.agent.voice_agent.service.DocumentIngestionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/rag/documents")
public class RagDocumentController {

    private final DocumentIngestionService documentIngestionService;

    public RagDocumentController(
            DocumentIngestionService documentIngestionService
    ) {
        this.documentIngestionService = documentIngestionService;
    }

    @PostMapping(
            value = "/ingest",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentIngestionResponse> ingest(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String documentText =
                new String(
                        file.getBytes(),
                        StandardCharsets.UTF_8
                );

        List<EmbeddedChunk> chunks =
                documentIngestionService.ingest(documentText);

        DocumentIngestionResponse response =
                new DocumentIngestionResponse(
                        file.getOriginalFilename(),
                        chunks.size(),
                        chunks
                );

        return ResponseEntity.ok(response);
    }
}