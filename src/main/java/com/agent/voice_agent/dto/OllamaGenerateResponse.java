package com.agent.voice_agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaGenerateResponse(
        String response,

        // All *_duration fields are in nanoseconds (Ollama's unit).
        @JsonProperty("total_duration")        Long totalDuration,
        @JsonProperty("load_duration")         Long loadDuration,
        @JsonProperty("prompt_eval_count")     Integer promptEvalCount,
        @JsonProperty("prompt_eval_duration")  Long promptEvalDuration,
        @JsonProperty("eval_count")            Integer evalCount,
        @JsonProperty("eval_duration")         Long evalDuration
) {
}