package com.agent.voice_agent.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.List;

@ConfigurationProperties(prefix = "audio")
public record AudioProperties(
        DataSize maxFileSize,
        List<String> allowedContentTypes) {

}
