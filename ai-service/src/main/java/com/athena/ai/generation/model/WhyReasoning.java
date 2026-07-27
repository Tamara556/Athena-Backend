package com.athena.ai.generation.model;

import java.util.List;

public record WhyReasoning(List<ReasoningEvent> events, String conclusion) {

    public record ReasoningEvent(String icon, String label, String text) {
    }
}
