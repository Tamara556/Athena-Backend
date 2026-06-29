package com.athena.ai.service;

import com.athena.ai.model.AssessmentQuestions;
import com.athena.ai.model.GoalAnalysis;
import com.athena.ai.model.InterviewEvaluation;
import com.athena.ai.model.InterviewQuestions;
import com.athena.ai.model.DailyPlanContent;
import com.athena.ai.model.LearningSessionContent;
import com.athena.ai.model.RoadmapContent;
import com.athena.common.event.BadgeSuggestion;

import java.util.List;
import java.util.UUID;

public interface AiGenerationService {

    AssessmentQuestions generateAssessment(UUID userId, String goal);

    GoalAnalysis analyzeGoal(UUID userId, String goal, String answersText);

    RoadmapContent generateRoadmap(UUID userId, String goal, GoalAnalysis analysis);

    DailyPlanContent generateDailyPlan(UUID userId, String goal, String level,
                                       String firstPhase, double dailyHours);

    LearningSessionContent generateLearningSession(UUID userId, String goal, String domain, String level,
                                                   String nodeTitle, String objectivesText);

    InterviewQuestions generateInterviewQuestions(UUID userId, String domain, String level);

    InterviewEvaluation evaluateInterview(UUID userId, String domain, String qaText);

    List<BadgeSuggestion> generateBadgeSuggestions(UUID userId, String domain);
}
