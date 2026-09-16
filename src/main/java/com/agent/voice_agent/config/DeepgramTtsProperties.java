package com.agent.voice_agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "deepgram.tts")
public record DeepgramTtsProperties(
        String model,

        // Playback speed multiplier. Deepgram accepts roughly 0.7 – 1.5.
        // 1.0 preserves natural pacing; slightly above 1.0 shortens both
        // synthesis and playback proportionally.
        @DefaultValue("1.0") Double speed,

        // Output container/codec. "mp3" is smallest for voice use.
        @DefaultValue("mp3") String encoding,

        // MP3 bitrate (bits per second). 24000 is sufficient for speech.
        @DefaultValue("24000") Integer bitRate
) {
}