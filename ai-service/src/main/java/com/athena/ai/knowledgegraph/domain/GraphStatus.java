package com.athena.ai.knowledgegraph.domain;

public enum GraphStatus {
    MASTERED,
    LEARNING,
    WEAKNESS;

    private static final int MASTERED_THRESHOLD = 85;
    private static final int LEARNING_THRESHOLD = 50;

    public static GraphStatus fromMastery(int mastery) {
        if (mastery >= MASTERED_THRESHOLD) {
            return MASTERED;
        }
        return mastery >= LEARNING_THRESHOLD ? LEARNING : WEAKNESS;
    }
}
