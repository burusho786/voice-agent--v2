package com.agent.voice_agent.dto;

public record VoiceConversationResponse(
        String transcript,
        String response
) {
}