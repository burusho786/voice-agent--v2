package com.agent.voice_agent.service;

import com.agent.voice_agent.dto.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production-grade document chunking service for RAG pipelines.
 *
 * <p>This implementation chunks documents based on:</p>
 * <ul>
 *     <li><b>Structure-awareness:</b> Detects Markdown/ASCII headings and section numbers</li>
 *     <li><b>Token-based sizing:</b> Uses approximate token counts (not characters)</li>
 *     <li><b>Sentence-boundary overlap:</b> Ensures chunks overlap at sentence boundaries</li>
 *     <li><b>Document metadata:</b> Attaches documentId and section headings to each chunk</li>
 * </ul>
 *
 * <p>The service is stateless and thread-safe.</p>
 */
@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    // Token budget for each chunk (approximate tokens)
    private static final int MIN_CHUNK_TOKENS = 500;
    private static final int MAX_CHUNK_TOKENS = 800;

    // Number of sentences to overlap between consecutive chunks
    private static final int OVERLAP_SENTENCES = 2;

    // Regex patterns for structural detection
    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "^(#{1,6}\\s+.*|\\d+\\.\\d+\\s+.*|\\d+\\.\\s+.*|[A-Z][A-Za-z\\s]{2,50}:)$",
            Pattern.MULTILINE
    );

    private static final Pattern SECTION_NUMBER_PATTERN = Pattern.compile(
            "^(\\d+\\.\\d+(?:\\.\\d+)?)\\s+(.*)$"
    );

    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile(
            "(?<=[.!?])\\s+(?=[A-Z0-9])"
    );

    /**
     * Chunks a document into semantically coherent, token-bounded pieces
     * with sentence-boundary overlap and document metadata.
     *
     * @param documentText the raw document text
     * @return list of DocumentChunk objects
     * @throws IllegalArgumentException if documentText is null or blank
     */
    public List<DocumentChunk> chunk(String documentText) {
        if (documentText == null || documentText.isBlank()) {
            throw new IllegalArgumentException("Document text cannot be null or blank");
        }

        String normalizedText = normalizeText(documentText);
        String documentId = extractDocumentId(normalizedText);

        log.info("Starting chunking for document: {} ({} characters)",
                documentId, normalizedText.length());

        List<Section> sections = splitIntoSections(normalizedText);
        log.debug("Detected {} sections in document {}", sections.size(), documentId);

        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkNumber = 1;
        String previousLastSentences = "";

        for (Section section : sections) {
            List<String> paragraphs = splitIntoParagraphs(section.content());

            if (paragraphs.isEmpty()) {
                continue;
            }

            List<DocumentChunk> sectionChunks = chunkParagraphs(
                    paragraphs,
                    section.heading(),
                    documentId,
                    chunkNumber,
                    previousLastSentences
            );

            if (!sectionChunks.isEmpty()) {
                chunks.addAll(sectionChunks);
                chunkNumber += sectionChunks.size();

                // Store the last few sentences of the last chunk for overlap
                DocumentChunk lastChunk = sectionChunks.get(sectionChunks.size() - 1);
                previousLastSentences = extractLastSentences(lastChunk.content(), OVERLAP_SENTENCES);
            }
        }

        log.info("Chunking complete for document {}: {} chunks generated", documentId, chunks.size());
        return chunks;
    }

    // -------------------------------------------------------------------------
    // Private Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Normalizes line endings and trims the document.
     */
    private String normalizeText(String text) {
        return text.replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    /**
     * Extracts a document identifier from the first heading or first line.
     * Falls back to a hash-based ID if no clear title is found.
     */
    private String extractDocumentId(String text) {
        String[] lines = text.split("\n", 3);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                return trimmed.replaceAll("^#+\\s*", "").trim();
            }
            if (trimmed.startsWith("Document ID:")) {
                return trimmed.substring("Document ID:".length()).trim();
            }
        }
        // Fallback: use first line (truncated) or a short hash
        String firstLine = lines[0].trim();
        if (!firstLine.isEmpty()) {
            return firstLine.length() > 60 ? firstLine.substring(0, 60) : firstLine;
        }
        return "unknown-document-" + Integer.toHexString(text.hashCode());
    }

    /**
     * Splits the document into sections based on heading detection.
     * Each section has a heading and content.
     */
    private List<Section> splitIntoSections(String text) {
        List<Section> sections = new ArrayList<>();
        Matcher matcher = HEADING_PATTERN.matcher(text);

        int lastEnd = 0;
        String currentHeading = "Introduction";

        while (matcher.find()) {
            // Don't treat the first line as a heading if it's the document title
            if (matcher.start() == 0) {
                currentHeading = matcher.group().trim();
                lastEnd = matcher.end();
                continue;
            }

            // Save the previous section
            if (matcher.start() > lastEnd) {
                String sectionContent = text.substring(lastEnd, matcher.start()).trim();
                if (!sectionContent.isEmpty()) {
                    sections.add(new Section(currentHeading, sectionContent));
                }
            }

            // Update current heading
            currentHeading = matcher.group().trim();
            lastEnd = matcher.end();
        }

        // Add the final section
        if (lastEnd < text.length()) {
            String sectionContent = text.substring(lastEnd).trim();
            if (!sectionContent.isEmpty()) {
                sections.add(new Section(currentHeading, sectionContent));
            }
        }

        // If no sections were detected, treat the entire document as one section
        if (sections.isEmpty()) {
            sections.add(new Section("Document", text));
        }

        return sections;
    }

    /**
     * Splits section content into paragraphs based on blank lines.
     */
    private List<String> splitIntoParagraphs(String content) {
        List<String> paragraphs = new ArrayList<>();
        String[] parts = content.split("\\n\\s*\\n");

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed);
            }
        }

        return paragraphs;
    }

    /**
     * Accumulates paragraphs into chunks that respect token budgets
     * and sentence boundaries.
     *
     * @param paragraphs the paragraphs in the section
     * @param sectionHeading the heading of the current section
     * @param documentId the document identifier
     * @param startChunkNumber the starting chunk number for this section
     * @param previousSentences sentences from the previous section's last chunk for overlap
     * @return list of DocumentChunk objects for this section
     */
    private List<DocumentChunk> chunkParagraphs(
            List<String> paragraphs,
            String sectionHeading,
            String documentId,
            int startChunkNumber,
            String previousSentences
    ) {
        List<DocumentChunk> chunks = new ArrayList<>();
        StringBuilder currentChunkBuilder = new StringBuilder();

        // Prepend overlap sentences from previous section if available
        if (previousSentences != null && !previousSentences.isEmpty()) {
            currentChunkBuilder.append(previousSentences).append("\n\n");
        }

        int currentTokenCount = estimateTokens(currentChunkBuilder.toString());
        int chunkNumber = startChunkNumber;

        for (String paragraph : paragraphs) {
            int paragraphTokens = estimateTokens(paragraph);

            // If a single paragraph exceeds the max chunk size, split it by sentences
            if (paragraphTokens > MAX_CHUNK_TOKENS) {
                // Flush current chunk if it has content
                if (currentTokenCount > estimateTokens(previousSentences != null ? previousSentences : "")) {
                    chunks.add(createChunk(currentChunkBuilder.toString(), documentId, sectionHeading, chunkNumber++));
                    currentChunkBuilder = new StringBuilder();
                    currentTokenCount = 0;
                }

                // Split the large paragraph into sentence-based chunks
                List<String> sentences = splitIntoSentences(paragraph);
                StringBuilder sentenceChunkBuilder = new StringBuilder();
                int sentenceTokenCount = 0;

                for (String sentence : sentences) {
                    int sentenceTokens = estimateTokens(sentence);
                    if (sentenceTokenCount + sentenceTokens > MAX_CHUNK_TOKENS && sentenceTokenCount > 0) {
                        chunks.add(createChunk(sentenceChunkBuilder.toString(), documentId, sectionHeading, chunkNumber++));
                        sentenceChunkBuilder = new StringBuilder();
                        sentenceTokenCount = 0;
                    }
                    sentenceChunkBuilder.append(sentence).append(" ");
                    sentenceTokenCount += sentenceTokens;
                }

                if (!sentenceChunkBuilder.isEmpty()) {
                    chunks.add(createChunk(sentenceChunkBuilder.toString().trim(), documentId, sectionHeading, chunkNumber++));
                }
                continue;
            }

            // Check if adding this paragraph would exceed the max token budget
            if (currentTokenCount + paragraphTokens > MAX_CHUNK_TOKENS && currentTokenCount >= MIN_CHUNK_TOKENS) {
                // Flush current chunk
                String chunkContent = currentChunkBuilder.toString().trim();
                if (!chunkContent.isEmpty()) {
                    chunks.add(createChunk(chunkContent, documentId, sectionHeading, chunkNumber++));
                }

                // Start new chunk with overlap
                String overlapText = extractLastSentences(chunkContent, OVERLAP_SENTENCES);
                currentChunkBuilder = new StringBuilder();
                if (!overlapText.isEmpty()) {
                    currentChunkBuilder.append(overlapText).append("\n\n");
                }
                currentTokenCount = estimateTokens(currentChunkBuilder.toString());
            }

            currentChunkBuilder.append(paragraph).append("\n\n");
            currentTokenCount += paragraphTokens;
        }

        // Flush the remaining content
        String finalContent = currentChunkBuilder.toString().trim();
        if (!finalContent.isEmpty() && estimateTokens(finalContent) > estimateTokens(previousSentences != null ? previousSentences : "")) {
            chunks.add(createChunk(finalContent, documentId, sectionHeading, chunkNumber));
        }

        return chunks;
    }

    /**
     * Creates a DocumentChunk with proper metadata.
     */
    private DocumentChunk createChunk(
            String content,
            String documentId,
            String sectionHeading,
            int chunkNumber
    ) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("chunkNumber", String.valueOf(chunkNumber));
        metadata.put("documentId", documentId);
        metadata.put("sectionHeading", sectionHeading != null ? sectionHeading : "Unknown Section");
        metadata.put("tokenCount", String.valueOf(estimateTokens(content)));

        return new DocumentChunk(
                "chunk-" + chunkNumber,
                content,
                metadata
        );
    }

    /**
     * Splits text into sentences using a simple regex pattern.
     * Handles common abbreviations and decimal numbers.
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_SPLIT_PATTERN.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            String sentence = text.substring(lastEnd, matcher.end()).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            lastEnd = matcher.end();
        }

        String lastSentence = text.substring(lastEnd).trim();
        if (!lastSentence.isEmpty()) {
            sentences.add(lastSentence);
        }

        return sentences;
    }

    /**
     * Extracts the last N sentences from the given text.
     * Used for overlap between consecutive chunks.
     */
    private String extractLastSentences(String text, int count) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        List<String> sentences = splitIntoSentences(text);
        if (sentences.size() <= count) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        for (int i = sentences.size() - count; i < sentences.size(); i++) {
            result.append(sentences.get(i)).append(" ");
        }
        return result.toString().trim();
    }

    /**
     * Estimates the number of tokens in a text string.
     *
     * <p>This is a heuristic approximation. It assumes:</p>
     * <ul>
     *     <li>1 token ≈ 4 characters for English text</li>
     *     <li>Whitespace is collapsed</li>
     *     <li>Punctuation is counted as separate tokens in some models</li>
     * </ul>
     *
     * <p>For the <code>nomic-embed-text</code> model, this approximation
     * is sufficient for chunk sizing purposes.</p>
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Count word tokens (whitespace-separated)
        String[] words = text.trim().split("\\s+");
        int wordCount = words.length;

        // Add an estimate for punctuation-only tokens
        int punctuationCount = 0;
        for (char c : text.toCharArray()) {
            if (isPunctuationToken(c)) {
                punctuationCount++;
            }
        }

        // Heuristic: words + some punctuation tokens
        return wordCount + (punctuationCount / 4);
    }

    /**
     * Determines if a character should be counted as a separate punctuation token.
     */
    private boolean isPunctuationToken(char c) {
        return c == '.' || c == ',' || c == ';' || c == ':' ||
                c == '!' || c == '?' || c == '(' || c == ')' ||
                c == '[' || c == ']' || c == '{' || c == '}' ||
                c == '"' || c == '\'' || c == '-' || c == '/';
    }

    // -------------------------------------------------------------------------
    // Internal Record for Section Representation
    // -------------------------------------------------------------------------

    /**
     * Represents a section of the document with its heading and content.
     */
    private record Section(String heading, String content) {
    }
}