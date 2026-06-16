package com.athena.ai.model;

import java.util.List;

public record InterviewQuestions(List<Question> questions) {

    public record Question(String type, String question) {
    }
}
