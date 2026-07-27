package com.athena.ai.learningsession.domain;

public enum Difficulty {
    EASY,
    MODERATE,
    CHALLENGING;

    public static Difficulty fromString(String value) {
        if (value == null) {
            return MODERATE;
        }
        try {
            return Difficulty.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MODERATE;
        }
    }

    public Difficulty easier() {
        return switch (this) {
            case CHALLENGING -> MODERATE;
            case MODERATE, EASY -> EASY;
        };
    }

    public Difficulty harder() {
        return switch (this) {
            case EASY -> MODERATE;
            case MODERATE, CHALLENGING -> CHALLENGING;
        };
    }

    public static Difficulty min(Difficulty a, Difficulty b) {
        return a.ordinal() <= b.ordinal() ? a : b;
    }
}
