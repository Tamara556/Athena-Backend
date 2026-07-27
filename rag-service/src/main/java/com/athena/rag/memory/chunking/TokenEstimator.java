package com.athena.rag.memory.chunking;

import org.springframework.stereotype.Component;

@Component
public class TokenEstimator {

    private static final double CHARS_PER_TOKEN = 4.0;

    public int estimate(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }
}
