package com.athena.ai.service.impl;

import com.athena.ai.client.dto.ResponseFormat;
import com.athena.ai.constants.AiConstants;
import com.athena.ai.llm.LlmService;
import com.athena.ai.llm.PromptTemplateService;
import com.athena.ai.model.AssessmentQuestions;
import com.athena.ai.model.BadgeSuggestions;
import com.athena.ai.model.DailyPlanContent;
import com.athena.ai.model.GoalAnalysis;
import com.athena.ai.model.InterviewEvaluation;
import com.athena.ai.model.InterviewQuestions;
import com.athena.ai.model.LearningSessionContent;
import com.athena.ai.model.RoadmapContent;
import com.athena.ai.service.AiGenerationService;
import com.athena.common.event.BadgeSuggestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiGenerationServiceImpl implements AiGenerationService {

    private final PromptTemplateService prompts;
    private final LlmService llm;

    private static final Map<String, Object> STR = Map.of("type", "string");
    private static final Map<String, Object> INT = Map.of("type", "integer");
    private static final Map<String, Object> NUM = Map.of("type", "number");

    @Override
    public AssessmentQuestions generateAssessment(UUID userId, String goal) {
        return llm.generateJson(userId, AiConstants.PROMPT_ASSESSMENT, system(),
                prompts.render(AiConstants.TPL_ASSESSMENT, Map.of("goal", goal)),
                AssessmentQuestions.class, ResponseFormat.ofSchema("assessment", ASSESSMENT_SCHEMA));
    }

    @Override
    public GoalAnalysis analyzeGoal(UUID userId, String goal, String answersText) {
        return llm.generateJson(userId, AiConstants.PROMPT_GOAL_ANALYSIS, system(),
                prompts.render(AiConstants.TPL_GOAL_ANALYSIS, Map.of("goal", goal, "answers", answersText)),
                GoalAnalysis.class, ResponseFormat.ofSchema("goal_analysis", GOAL_ANALYSIS_SCHEMA));
    }

    @Override
    public RoadmapContent generateRoadmap(UUID userId, String goal, GoalAnalysis analysis) {
        Map<String, String> vars = Map.of(
                "goal", goal,
                "level", analysis.level(),
                "dailyHours", String.valueOf(analysis.dailyHours()),
                "prerequisites", joinOrNone(analysis.prerequisites()));
        RoadmapContent content = llm.generateJson(userId, AiConstants.PROMPT_ROADMAP, system(),
                prompts.render(AiConstants.TPL_ROADMAP, vars), RoadmapContent.class,
                ResponseFormat.ofSchema("roadmap", ROADMAP_SCHEMA));
        return withStatuses(content);
    }

    @Override
    public DailyPlanContent generateDailyPlan(UUID userId, String goal, String level,
                                              String firstPhase, double dailyHours) {
        Map<String, String> vars = Map.of(
                "goal", goal,
                "level", level,
                "dailyHours", String.valueOf(dailyHours),
                "phase", firstPhase);
        return llm.generateJson(userId, AiConstants.PROMPT_DAILY_PLAN, system(),
                prompts.render(AiConstants.TPL_DAILY_PLAN, vars), DailyPlanContent.class,
                ResponseFormat.ofSchema("daily_plan", DAILY_PLAN_SCHEMA));
    }

    @Override
    public LearningSessionContent generateLearningSession(UUID userId, String goal, String domain, String level,
                                                          String nodeTitle, String objectivesText) {
        Map<String, String> vars = Map.of(
                "goal", goal,
                "domain", domain,
                "level", level,
                "node", nodeTitle,
                "objectives", objectivesText);
        return llm.generateJson(userId, AiConstants.PROMPT_LEARNING_SESSION, system(),
                prompts.render(AiConstants.TPL_LEARNING_SESSION, vars), LearningSessionContent.class,
                ResponseFormat.ofSchema("learning_session", LEARNING_SESSION_SCHEMA));
    }

    private RoadmapContent withStatuses(RoadmapContent content) {
        if (content == null || content.phases() == null) {
            return content;
        }
        List<RoadmapContent.Phase> phases = content.phases();
        List<RoadmapContent.Phase> enriched = new ArrayList<>(phases.size());
        for (int i = 0; i < phases.size(); i++) {
            RoadmapContent.Phase p = phases.get(i);
            String status = i == 0 ? "CURRENT" : i == 1 ? "AVAILABLE" : "LOCKED";
            enriched.add(new RoadmapContent.Phase(p.name(), p.description(), p.durationWeeks(), p.objectives(), status));
        }
        return new RoadmapContent(enriched);
    }

    @Override
    public InterviewQuestions generateInterviewQuestions(UUID userId, String domain, String level) {
        return llm.generateJson(userId, AiConstants.PROMPT_INTERVIEW_QUESTIONS, system(),
                prompts.render(AiConstants.TPL_INTERVIEW_QUESTIONS, Map.of("domain", domain, "level", level)),
                InterviewQuestions.class);
    }

    @Override
    public InterviewEvaluation evaluateInterview(UUID userId, String domain, String qaText) {
        return llm.generateJson(userId, AiConstants.PROMPT_INTERVIEW_EVALUATION, system(),
                prompts.render(AiConstants.TPL_INTERVIEW_EVALUATION, Map.of("domain", domain, "qa", qaText)),
                InterviewEvaluation.class);
    }

    @Override
    public List<BadgeSuggestion> generateBadgeSuggestions(UUID userId, String domain) {
        BadgeSuggestions result = llm.generateJson(userId, AiConstants.PROMPT_BADGE_SUGGESTIONS, system(),
                prompts.render(AiConstants.TPL_BADGE_SUGGESTIONS, Map.of("domain", domain)),
                BadgeSuggestions.class);
        return result.badges() == null ? List.of() : result.badges();
    }

    private String system() {
        return prompts.render(AiConstants.TPL_SYSTEM, Map.of());
    }

    private String joinOrNone(List<String> values) {
        return values == null || values.isEmpty() ? "none" : String.join(", ", values);
    }

    private static Map<String, Object> arrayOf(Map<String, Object> items) {
        return Map.of("type", "array", "items", items);
    }

    private static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        return Map.of("type", "object", "properties", properties, "required", required);
    }

    private static Map<String, Object> enumOf(List<String> values) {
        return Map.of("type", "string", "enum", values);
    }

    private static final Map<String, Object> ASSESSMENT_SCHEMA =
            object(Map.of("questions", arrayOf(STR)), List.of("questions"));

    private static final Map<String, Object> GOAL_ANALYSIS_SCHEMA = object(
            Map.of("domain", STR, "level", STR, "estimatedMonths", INT, "dailyHours", NUM,
                    "prerequisites", arrayOf(STR)),
            List.of("domain", "level", "estimatedMonths", "dailyHours", "prerequisites"));

    private static final Map<String, Object> ROADMAP_SCHEMA = object(
            Map.of("phases", arrayOf(object(
                    Map.of("name", STR, "description", STR, "durationWeeks", INT, "objectives", arrayOf(STR)),
                    List.of("name", "description", "durationWeeks", "objectives")))),
            List.of("phases"));

    private static final Map<String, Object> DAILY_PLAN_SCHEMA = object(
            Map.of("items", arrayOf(object(
                    Map.of("type", STR, "title", STR, "description", STR, "estimatedMinutes", INT),
                    List.of("type", "title", "description", "estimatedMinutes")))),
            List.of("items"));

    private static final Map<String, Object> READING_SCHEMA = object(
            Map.of("title", STR, "content", STR, "estimatedMinutes", INT),
            List.of("title", "content", "estimatedMinutes"));

    private static final Map<String, Object> WATCHING_SCHEMA = object(
            Map.of("title", STR, "description", STR, "videoQuery", STR, "estimatedMinutes", INT),
            List.of("title", "description", "videoQuery", "estimatedMinutes"));

    private static final Map<String, Object> PRACTICE_SCHEMA = object(
            Map.of("title", STR, "description", STR,
                    "practiceType", enumOf(List.of(
                            "CODE_EDITOR", "LANGUAGE_EXERCISE", "SCENARIO", "CREATIVE_PROMPT", "REFLECTION")),
                    "instructions", STR, "starterContent", STR, "estimatedMinutes", INT),
            List.of("title", "description", "practiceType", "instructions", "starterContent", "estimatedMinutes"));

    private static final Map<String, Object> QUIZ_SCHEMA = object(
            Map.of("question", STR,
                    "type", enumOf(List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE")),
                    "options", arrayOf(STR), "correctAnswer", STR, "explanation", STR),
            List.of("question", "type", "options", "correctAnswer", "explanation"));

    private static final Map<String, Object> LEARNING_SESSION_SCHEMA = object(
            Map.of("readings", arrayOf(READING_SCHEMA),
                    "watchings", arrayOf(WATCHING_SCHEMA),
                    "practices", arrayOf(PRACTICE_SCHEMA),
                    "quizzes", arrayOf(QUIZ_SCHEMA)),
            List.of("readings", "watchings", "practices", "quizzes"));
}
