package com.athena.ai.learningsession.domain;

public enum PracticeType {
    CODE_EDITOR,
    LANGUAGE_EXERCISE,
    SCENARIO,
    CREATIVE_PROMPT,
    REFLECTION;

    public static PracticeType fromString(String value) {
        if (value == null) {
            return REFLECTION;
        }
        try {
            return PracticeType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return REFLECTION;
        }
    }
}
