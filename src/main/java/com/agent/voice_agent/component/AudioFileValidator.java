package com.agent.voice_agent.component;

import com.agent.voice_agent.config.AudioProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AudioFileValidator {

    private final AudioProperties audioProperties;

    public AudioFileValidator(AudioProperties audioProperties) {
        this.audioProperties = audioProperties;
    }

    public void validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Audio file is empty");
        }

        validateSize(file);
        validateContentType(file);
    }

    private void validateSize(MultipartFile file) {

        if (file.getSize() > audioProperties.maxFileSize().toBytes()) {
            throw new IllegalArgumentException(
                    "Audio file exceeds the maximum allowed size of "
                            + audioProperties.maxFileSize()
            );
        }
    }

    private void validateContentType(MultipartFile file) {

        String contentType = file.getContentType();

        if (contentType == null
                || !audioProperties.allowedContentTypes()
                .contains(contentType.toLowerCase())) {

            throw new IllegalArgumentException(
                    "Unsupported audio content type: " + contentType
            );
        }
    }
}