package com.athena.ai.generation.service.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.generation.model.AssessmentQuestions;
import com.athena.ai.generation.model.BadgeSuggestions;
import com.athena.ai.generation.model.DailyMissionPlan;
import com.athena.ai.generation.model.DailyPlanContent;
import com.athena.ai.generation.model.DrillContent;
import com.athena.ai.generation.model.GoalAnalysis;
import com.athena.ai.generation.model.InterviewEvaluation;
import com.athena.ai.generation.model.InterviewQuestions;
import com.athena.ai.generation.model.LearningSessionContent;
import com.athena.ai.generation.model.MentorReply;
import com.athena.ai.generation.model.RoadmapContent;
import com.athena.ai.generation.model.WhyReasoning;
import com.athena.ai.generation.schema.GenerationSchemas;
import com.athena.ai.generation.service.AiGenerationService;
import com.athena.ai.llm.LlmService;
import com.athena.ai.llm.PromptTemplateService;
import com.athena.common.event.BadgeSuggestion;
import com.athena.llm.model.ResponseFormat;
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

    @Override
    public AssessmentQuestions generateAssessment(UUID userId, String goal) {
        return llm.generateJson(userId, AiConstants.PROMPT_ASSESSMENT, system(),
                prompts.render(AiConstants.TPL_ASSESSMENT, Map.of("goal", goal)),
                AssessmentQuestions.class, ResponseFormat.ofSchema("assessment", GenerationSchemas.ASSESSMENT));
    }

    @Override
    public GoalAnalysis analyzeGoal(UUID userId, String goal, String answersText) {
        return llm.generateJson(userId, AiConstants.PROMPT_GOAL_ANALYSIS, system(),
                prompts.render(AiConstants.TPL_GOAL_ANALYSIS, Map.of("goal", goal, "answers", answersText)),
                GoalAnalysis.class, ResponseFormat.ofSchema("goal_analysis", GenerationSchemas.GOAL_ANALYSIS));
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
                ResponseFormat.ofSchema("roadmap", GenerationSchemas.ROADMAP));
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
                ResponseFormat.ofSchema("daily_plan", GenerationSchemas.DAILY_PLAN));
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
                ResponseFormat.ofSchema("learning_session", GenerationSchemas.LEARNING_SESSION));
    }

    @Override
    public DailyMissionPlan generateDailyMission(UUID userId, String goal, String domain, String level,
                                                 String nodeTitle, String objectivesText, String weakAreasText,
                                                 String adjustmentRequest, int availableMinutes) {
        Map<String, String> vars = Map.of(
                "goal", goal,
                "domain", domain,
                "level", level,
                "node", nodeTitle,
                "objectives", objectivesText,
                "weakAreas", weakAreasText,
                "adjustRequest", adjustmentRequest,
                "availableMinutes", String.valueOf(availableMinutes));
        return llm.generateJson(userId, AiConstants.PROMPT_DAILY_MISSION, system(),
                prompts.render(AiConstants.TPL_DAILY_MISSION, vars), DailyMissionPlan.class,
                ResponseFormat.ofSchema("daily_mission", GenerationSchemas.DAILY_MISSION));
    }

    @Override
    public WhyReasoning generateWhyReasoning(UUID userId, String goal, String nodeTitle, String interviewText,
                                             String masteredSkill, String weakAreasText) {
        Map<String, String> vars = Map.of(
                "goal", goal,
                "node", nodeTitle,
                "interview", interviewText,
                "mastered", masteredSkill,
                "weakAreas", weakAreasText);
        return llm.generateJson(userId, AiConstants.PROMPT_WHY_REASONING, system(),
                prompts.render(AiConstants.TPL_WHY_REASONING, vars), WhyReasoning.class,
                ResponseFormat.ofSchema("why_reasoning", GenerationSchemas.WHY_REASONING));
    }

    @Override
    public DrillContent generateWeaknessDrill(UUID userId, String skillName, String domain, int masteryPercentage) {
        Map<String, String> vars = Map.of(
                "skill", skillName,
                "domain", domain,
                "mastery", String.valueOf(masteryPercentage));
        return llm.generateJson(userId, AiConstants.PROMPT_WEAKNESS_DRILL, system(),
                prompts.render(AiConstants.TPL_WEAKNESS_DRILL, vars), DrillContent.class,
                ResponseFormat.ofSchema("weakness_drill", GenerationSchemas.WEAKNESS_DRILL));
    }

    @Override
    public MentorReply generateMentorReply(UUID userId, String topic, String confidence) {
        Map<String, String> vars = Map.of("topic", topic, "confidence", confidence);
        return llm.generateJson(userId, AiConstants.PROMPT_MENTOR_REPLY, system(),
                prompts.render(AiConstants.TPL_MENTOR_REPLY, vars), MentorReply.class,
                ResponseFormat.ofSchema("mentor_reply", GenerationSchemas.MENTOR_REPLY));
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

    private String system() {
        return prompts.render(AiConstants.TPL_SYSTEM, Map.of());
    }

    private String joinOrNone(List<String> values) {
        return values == null || values.isEmpty() ? "none" : String.join(", ", values);
    }
}
