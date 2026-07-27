package com.athena.ai.generation.service;

import com.athena.ai.generation.model.AssessmentQuestions;
import com.athena.ai.generation.model.DailyMissionPlan;
import com.athena.ai.generation.model.DrillContent;
import com.athena.ai.generation.model.GoalAnalysis;
import com.athena.ai.generation.model.InterviewEvaluation;
import com.athena.ai.generation.model.InterviewQuestions;
import com.athena.ai.generation.model.DailyPlanContent;
import com.athena.ai.generation.model.LearningSessionContent;
import com.athena.ai.generation.model.MentorReply;
import com.athena.ai.generation.model.RoadmapContent;
import com.athena.ai.generation.model.WhyReasoning;
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

    DailyMissionPlan generateDailyMission(UUID userId, String goal, String domain, String level,
                                          String nodeTitle, String objectivesText, String weakAreasText,
                                          String adjustmentRequest, int availableMinutes);

    WhyReasoning generateWhyReasoning(UUID userId, String goal, String nodeTitle, String interviewText,
                                      String masteredSkill, String weakAreasText);

    DrillContent generateWeaknessDrill(UUID userId, String skillName, String domain, int masteryPercentage);

    MentorReply generateMentorReply(UUID userId, String topic, String confidence);

    InterviewQuestions generateInterviewQuestions(UUID userId, String domain, String level);

    InterviewEvaluation evaluateInterview(UUID userId, String domain, String qaText);

    List<BadgeSuggestion> generateBadgeSuggestions(UUID userId, String domain);
}
