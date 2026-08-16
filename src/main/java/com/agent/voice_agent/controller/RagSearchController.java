package com.agent.voice_agent.controller;

import com.agent.voice_agent.dto.RagSearchRequest;
import com.agent.voice_agent.dto.RagSearchResponse;
import com.agent.voice_agent.dto.RagSearchResult;
import com.agent.voice_agent.dto.VectorSearchResult;
import com.agent.voice_agent.service.RagRetrievalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RagSearchController {

    private final RagRetrievalService ragRetrievalService;

    public RagSearchController(
            RagRetrievalService ragRetrievalService
    ) {
        this.ragRetrievalService =
                ragRetrievalService;
    }

    @PostMapping("/search")
    public ResponseEntity<RagSearchResponse> search(
            @Valid @RequestBody RagSearchRequest request
    ) {

        int topK =
                request.topK() == null
                        ? 3
                        : request.topK();

        List<VectorSearchResult> results =
                ragRetrievalService.retrieve(
                        request.query(),
                        topK
                );

        List<RagSearchResult> responseResults =
                results.stream()
                        .map(result ->
                                new RagSearchResult(
                                        result.chunk().id(),
                                        result.chunk().content(),
                                        result.score(),
                                        result.chunk().metadata()
                                )
                        )
                        .toList();

        return ResponseEntity.ok(
                new RagSearchResponse(
                        request.query(),
                        responseResults
                )
        );
    }
}