package com.agent.voice_agent.dto;

public record VoiceAgentResponse(
        String transcript,
        String response,
        String audio,
        String audioMimeType
) {
}