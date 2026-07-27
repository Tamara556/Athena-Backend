package com.athena.rag.memory.chunking;

public record Chunk(int index, String content, int tokenCount) {
}
