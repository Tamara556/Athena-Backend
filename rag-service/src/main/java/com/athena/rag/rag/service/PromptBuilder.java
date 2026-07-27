package com.athena.rag.rag.service;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are Athena, a personal learning mentor. You answer questions about a single learner using
            ONLY the numbered CONTEXT provided below, which is drawn from that learner's own history:
            lessons, interviews, roadmap, knowledge graph, achievements and notes.

            Rules:
            - Use only facts contained in the CONTEXT. Never invent lessons, scores, or events.
            - Ground every claim in the sources and cite them inline as [n].
            - If the CONTEXT does not contain enough information to answer, say so plainly and do not guess.
            - Be concise, specific and encouraging, and speak directly to the learner.""";

    public String system() {
        return SYSTEM_PROMPT;
    }

    public String user(String question, String contextText) {
        return """
                CONTEXT:
                %s

                QUESTION:
                %s""".formatted(contextText, question);
    }
}
