package com.athena.rag.memory.ingestion;

import com.athena.common.event.InterviewEvaluatedEvent;
import com.athena.rag.client.dto.RoadmapView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemoryTextFactory {

    public String interview(InterviewEvaluatedEvent event) {
        String weaknesses = event.weaknesses() == null || event.weaknesses().isEmpty()
                ? "none recorded" : String.join(", ", event.weaknesses());
        return """
                Mock interview in the domain of %s.
                Score: %d out of 100.
                Outcome: %s.
                Weak areas identified during this interview: %s."""
                .formatted(event.domain(), event.score(),
                        event.passed() ? "passed" : "not yet passing", weaknesses);
    }

    public String roadmap(RoadmapView roadmap) {
        StringBuilder text = new StringBuilder();
        text.append("Learning roadmap for the goal: ").append(nullSafe(roadmap.goal()))
                .append(" (assessed level: ").append(nullSafe(roadmap.level())).append(").\n");
        List<RoadmapView.PhaseView> phases = roadmap.phases();
        if (phases != null) {
            for (RoadmapView.PhaseView phase : phases) {
                text.append("Phase: ").append(nullSafe(phase.name())).append(". ")
                        .append(nullSafe(phase.description()));
                if (phase.objectives() != null && !phase.objectives().isEmpty()) {
                    text.append(" Objectives: ").append(String.join("; ", phase.objectives())).append('.');
                }
                text.append('\n');
            }
        }
        return text.toString().strip();
    }

    public String achievement(String badgeCode) {
        return "Achievement unlocked: the learner earned the '" + badgeCode + "' badge, "
                + "marking a milestone in their learning journey.";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
