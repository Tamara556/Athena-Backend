package com.athena.ai.dailyjourney.domain;

public enum AdjustAction {
    SIMPLIFY,
    INTENSIFY,
    REGENERATE;

    public static AdjustAction fromString(String value) {
        if (value == null) {
            return REGENERATE;
        }
        try {
            return AdjustAction.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return REGENERATE;
        }
    }
}
