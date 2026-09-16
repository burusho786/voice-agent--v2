package com.agent.voice_agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaGenerateRequest(
        String model,
        String prompt,
        boolean stream,
        @JsonProperty("keep_alive") String keepAlive,
        Options options
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Options(
            @JsonProperty("num_predict") Integer numPredict,
            Double temperature
    ) {
    }
}