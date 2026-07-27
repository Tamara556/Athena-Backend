package com.athena.ai.dailyjourney.domain;

public enum ConfidenceLevel {
    CONFIDENT,
    UNSURE,
    NEED_HELP;

    public static ConfidenceLevel fromString(String value) {
        if (value == null) {
            return UNSURE;
        }
        try {
            return ConfidenceLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNSURE;
        }
    }
}
