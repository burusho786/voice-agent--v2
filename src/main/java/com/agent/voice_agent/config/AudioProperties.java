package com.agent.voice_agent.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "audio")
public record AudioProperties(
        String maxFileSize,
        List<String> allowedContentTypes) {

}
