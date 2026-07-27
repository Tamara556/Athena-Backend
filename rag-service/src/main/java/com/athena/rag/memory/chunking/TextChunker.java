package com.athena.rag.memory.chunking;

import com.athena.rag.config.RagProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private static final double TOKENS_PER_WORD = 1.3;

    private final RagProperties properties;
    private final TokenEstimator tokenEstimator;

    public TextChunker(RagProperties properties, TokenEstimator tokenEstimator) {
        this.properties = properties;
        this.tokenEstimator = tokenEstimator;
    }

    public List<Chunk> chunk(String content) {
        String trimmed = content == null ? "" : content.strip();
        if (trimmed.isEmpty()) {
            return List.of();
        }

        String[] words = trimmed.split("\\s+");
        int wordsPerChunk = Math.max(1, (int) Math.floor(properties.chunkMaxTokens() / TOKENS_PER_WORD));
        int overlapWords = Math.min(wordsPerChunk - 1,
                Math.max(0, (int) Math.floor(properties.chunkOverlapTokens() / TOKENS_PER_WORD)));
        int step = Math.max(1, wordsPerChunk - overlapWords);

        List<Chunk> chunks = new ArrayList<>();
        int index = 0;
        for (int start = 0; start < words.length; start += step) {
            int end = Math.min(start + wordsPerChunk, words.length);
            String text = String.join(" ", java.util.Arrays.copyOfRange(words, start, end));
            chunks.add(new Chunk(index++, text, tokenEstimator.estimate(text)));
            if (end == words.length) {
                break;
            }
        }
        return chunks;
    }
}
