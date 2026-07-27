package com.athena.ai.dailyjourney.domain;

public enum BlockType {
    READING,
    PRACTICE,
    VIDEO,
    QUIZ,
    SPEAKING,
    REVIEW,
    DRILL;

    public static BlockType fromString(String value) {
        if (value == null) {
            return READING;
        }
        try {
            return BlockType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return READING;
        }
    }
}
