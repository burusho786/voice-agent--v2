package com.agent.voice_agent.component;

import com.agent.voice_agent.config.AudioProperties;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
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

        String rawContentType = file.getContentType();

        if (rawContentType == null || rawContentType.isBlank()) {
            throw new IllegalArgumentException("Audio content type is missing");
        }

        /*
         * Browsers send Content-Type with parameters, e.g.
         *   audio/webm;codecs=opus
         *   audio/ogg;codecs=opus
         * We must compare only the base media type
         * (audio/webm, audio/ogg, ...) against the allow-list.
         */
        String baseType;
        try {
            MediaType mediaType = MediaType.parseMediaType(rawContentType);
            baseType = (mediaType.getType() + "/" + mediaType.getSubtype())
                    .toLowerCase();
        } catch (InvalidMediaTypeException e) {
            throw new IllegalArgumentException(
                    "Invalid audio content type: " + rawContentType
            );
        }

        if (!audioProperties.allowedContentTypes().contains(baseType)) {
            throw new IllegalArgumentException(
                    "Unsupported audio content type: " + rawContentType
            );
        }
    }
}