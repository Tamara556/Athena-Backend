package com.athena.ai.learningsession.domain;

public enum QuizType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TRUE_FALSE;

    public static QuizType fromString(String value) {
        if (value == null) {
            return SINGLE_CHOICE;
        }
        try {
            return QuizType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return SINGLE_CHOICE;
        }
    }
}
