package com.athena.ai.constants;

public final class AiConstants {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    public static final String PROMPT_ASSESSMENT = "ASSESSMENT";
    public static final String PROMPT_GOAL_ANALYSIS = "GOAL_ANALYSIS";
    public static final String PROMPT_ROADMAP = "ROADMAP";
    public static final String PROMPT_DAILY_PLAN = "DAILY_PLAN";
    public static final String PROMPT_INTERVIEW_QUESTIONS = "INTERVIEW_QUESTIONS";
    public static final String PROMPT_INTERVIEW_EVALUATION = "INTERVIEW_EVALUATION";
    public static final String PROMPT_BADGE_SUGGESTIONS = "BADGE_SUGGESTIONS";

    public static final String TPL_SYSTEM = "system";
    public static final String TPL_ASSESSMENT = "assessment";
    public static final String TPL_GOAL_ANALYSIS = "goal-analysis";
    public static final String TPL_ROADMAP = "roadmap";
    public static final String TPL_DAILY_PLAN = "daily-plan";
    public static final String TPL_INTERVIEW_QUESTIONS = "interview-questions";
    public static final String TPL_INTERVIEW_EVALUATION = "interview-evaluation";
    public static final String TPL_BADGE_SUGGESTIONS = "badge-suggestions";

    public static final String RESOURCE_ONBOARDING = "Onboarding session";
    public static final String RESOURCE_ROADMAP = "Roadmap";
    public static final String RESOURCE_DAILY_PLAN = "Daily plan";
    public static final String RESOURCE_RETRY = "AI retry request";

    public static final String RETRY_ASSESSMENT = "ONBOARDING_ASSESSMENT";
    public static final String RETRY_COMPLETE = "ONBOARDING_COMPLETE";

    public static final int MAX_RETRIES = 5;

    public static final String CACHE_VISUALIZATION = "knowledge-graph:visualization";
    public static final String CACHE_HISTORY = "knowledge-graph:history";
    public static final String RESOURCE_VISUALIZATION = "Knowledge graph";
    public static final int SNAPSHOT_SIGNIFICANT_DELTA = 5;

    public static final String METRIC_VIZ_GENERATION = "athena.kg.visualization.generation";
    public static final String METRIC_VIZ_REQUESTS = "athena.kg.visualization.requests";
    public static final String METRIC_VIZ_CACHE_MISS = "athena.kg.visualization.cache.miss";
    public static final String METRIC_SNAPSHOTS_CREATED = "athena.kg.snapshots.created";

    public static final int GOAL_MAX_LENGTH = 1000;

    public static final String QUESTION_REQUIRED = "question is required";
    public static final String ANSWER_REQUIRED = "answer is required";
    public static final String USER_ID_REQUIRED = "userId is required";
    public static final String DOMAIN_REQUIRED = "domain is required";
    public static final String ANSWERS_NOT_EMPTY = "answers must not be empty";
    public static final String NODES_NOT_EMPTY = "nodes must not be empty";
    public static final String SKILL_NAME_REQUIRED = "skillName is required";
    public static final String MASTERY_PERCENTAGE_MIN_MESSAGE = "masteryPercentage must be >= 0";
    public static final String MASTERY_PERCENTAGE_MAX_MESSAGE = "masteryPercentage must be <= 100";
    public static final String CONFIDENCE_SCORE_MIN_MESSAGE = "confidenceScore must be >= 0";
    public static final String CONFIDENCE_SCORE_MAX_MESSAGE = "confidenceScore must be <= 1";
    public static final String GOAL_REQUIRED = "goal is required";
    public static final String GOAL_MAX_LENGTH_MESSAGE =
            "goal must be at most " + GOAL_MAX_LENGTH + " characters";
}
