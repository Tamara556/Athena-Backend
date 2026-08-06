package com.athena.ai.generation.schema;

import java.util.List;
import java.util.Map;

import static com.athena.ai.llm.JsonSchema.INT;
import static com.athena.ai.llm.JsonSchema.NUM;
import static com.athena.ai.llm.JsonSchema.STR;
import static com.athena.ai.llm.JsonSchema.arrayOf;
import static com.athena.ai.llm.JsonSchema.enumOf;
import static com.athena.ai.llm.JsonSchema.object;

/**
 * JSON-schema definitions for every structured generation output. These constrain the LLM
 * response so it deserializes cleanly into the matching {@code com.athena.ai.generation.model} type.
 * Extracted from the generation service so orchestration and output contracts evolve independently.
 */
public final class GenerationSchemas {

    private GenerationSchemas() {
    }

    public static final Map<String, Object> ASSESSMENT =
            object(Map.of("questions", arrayOf(STR)), List.of("questions"));

    public static final Map<String, Object> GOAL_ANALYSIS = object(
            Map.of("domain", STR, "level", STR, "estimatedMonths", INT, "dailyHours", NUM,
                    "prerequisites", arrayOf(STR)),
            List.of("domain", "level", "estimatedMonths", "dailyHours", "prerequisites"));

    public static final Map<String, Object> ROADMAP = object(
            Map.of("phases", arrayOf(object(
                    Map.of("name", STR, "description", STR, "durationWeeks", INT, "objectives", arrayOf(STR)),
                    List.of("name", "description", "durationWeeks", "objectives")))),
            List.of("phases"));

    public static final Map<String, Object> DAILY_PLAN = object(
            Map.of("items", arrayOf(object(
                    Map.of("type", STR, "title", STR, "description", STR, "estimatedMinutes", INT),
                    List.of("type", "title", "description", "estimatedMinutes")))),
            List.of("items"));

    private static final Map<String, Object> READING = object(
            Map.of("title", STR, "content", STR, "estimatedMinutes", INT),
            List.of("title", "content", "estimatedMinutes"));

    private static final Map<String, Object> WATCHING = object(
            Map.of("title", STR, "description", STR, "videoQuery", STR, "videoId", STR, "estimatedMinutes", INT),
            List.of("title", "description", "videoQuery", "videoId", "estimatedMinutes"));

    private static final Map<String, Object> PRACTICE = object(
            Map.of("title", STR, "description", STR,
                    "practiceType", enumOf(List.of(
                            "CODE_EDITOR", "LANGUAGE_EXERCISE", "SCENARIO", "CREATIVE_PROMPT", "REFLECTION")),
                    "instructions", STR, "starterContent", STR, "estimatedMinutes", INT),
            List.of("title", "description", "practiceType", "instructions", "starterContent", "estimatedMinutes"));

    private static final Map<String, Object> QUIZ = object(
            Map.of("question", STR,
                    "type", enumOf(List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE")),
                    "options", arrayOf(STR), "correctAnswer", STR, "explanation", STR),
            List.of("question", "type", "options", "correctAnswer", "explanation"));

    public static final Map<String, Object> LEARNING_SESSION = object(
            Map.of("readings", arrayOf(READING),
                    "watchings", arrayOf(WATCHING),
                    "practices", arrayOf(PRACTICE),
                    "quizzes", arrayOf(QUIZ)),
            List.of("readings", "watchings", "practices", "quizzes"));

    private static final Map<String, Object> BLOCK_TYPE = enumOf(List.of(
            "READING", "PRACTICE", "VIDEO", "QUIZ", "SPEAKING", "REVIEW", "DRILL"));

    private static final Map<String, Object> DIFFICULTY = enumOf(List.of("EASY", "MODERATE", "CHALLENGING"));

    public static final Map<String, Object> DAILY_MISSION = object(
            Map.of("mission", object(
                            Map.of("title", STR, "description", STR, "goalContext", STR, "difficulty", DIFFICULTY),
                            List.of("title", "description", "goalContext", "difficulty")),
                    "blocks", arrayOf(object(
                            Map.of("type", BLOCK_TYPE, "title", STR, "description", STR,
                                    "difficulty", DIFFICULTY, "durationMinutes", INT),
                            List.of("type", "title", "description", "difficulty", "durationMinutes")))),
            List.of("mission", "blocks"));

    public static final Map<String, Object> WHY_REASONING = object(
            Map.of("events", arrayOf(object(
                            Map.of("icon", STR, "label", STR, "text", STR),
                            List.of("icon", "label", "text"))),
                    "conclusion", STR),
            List.of("events", "conclusion"));

    public static final Map<String, Object> WEAKNESS_DRILL = object(
            Map.of("title", STR, "description", STR, "type", BLOCK_TYPE, "durationMinutes", INT, "instructions", STR),
            List.of("title", "description", "type", "durationMinutes", "instructions"));

    public static final Map<String, Object> MENTOR_REPLY = object(
            Map.of("reply", STR), List.of("reply"));
}
