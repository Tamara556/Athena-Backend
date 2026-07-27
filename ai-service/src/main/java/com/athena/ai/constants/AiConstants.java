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
    public static final String PROMPT_LEARNING_SESSION = "LEARNING_SESSION";
    public static final String PROMPT_DAILY_MISSION = "DAILY_MISSION";
    public static final String PROMPT_WHY_REASONING = "WHY_REASONING";
    public static final String PROMPT_WEAKNESS_DRILL = "WEAKNESS_DRILL";
    public static final String PROMPT_MENTOR_REPLY = "MENTOR_REPLY";

    public static final String TPL_SYSTEM = "system";
    public static final String TPL_ASSESSMENT = "assessment";
    public static final String TPL_GOAL_ANALYSIS = "goal-analysis";
    public static final String TPL_ROADMAP = "roadmap";
    public static final String TPL_DAILY_PLAN = "daily-plan";
    public static final String TPL_INTERVIEW_QUESTIONS = "interview-questions";
    public static final String TPL_INTERVIEW_EVALUATION = "interview-evaluation";
    public static final String TPL_BADGE_SUGGESTIONS = "badge-suggestions";
    public static final String TPL_LEARNING_SESSION = "learning-session";
    public static final String TPL_DAILY_MISSION = "daily-mission";
    public static final String TPL_WHY_REASONING = "why-reasoning";
    public static final String TPL_WEAKNESS_DRILL = "weakness-drill";
    public static final String TPL_MENTOR_REPLY = "mentor-reply";

    public static final String RESOURCE_ONBOARDING = "Onboarding session";
    public static final String RESOURCE_ROADMAP = "Roadmap";
    public static final String RESOURCE_DAILY_PLAN = "Daily plan";
    public static final String RESOURCE_RETRY = "AI retry request";
    public static final String RESOURCE_LEARNING_SESSION = "Learning session";

    public static final String CACHE_LEARNING_SESSION = "learning-session";
    public static final int LEARNING_SESSION_BUFFER = 5;

    public static final String CACHE_DAILY_JOURNEY = "daily-journey";
    public static final String RESOURCE_DAILY_MISSION = "Daily mission";
    public static final String RESOURCE_DAILY_BLOCK = "Daily block";

    public static final int WEAKNESS_MASTERY_THRESHOLD = 70;
    public static final int DEFAULT_AVAILABLE_MINUTES = 105;
    public static final int MAX_WEAKNESS_BLOCKS = 2;
    public static final int SKIP_ADAPT_THRESHOLD = 3;
    public static final double FAST_COMPLETION_RATIO = 0.5;
    public static final int LOW_CONFIDENCE_PERCENT = 60;
    public static final int MASTERY_DRILL_DELTA = 4;
    public static final int MIN_BLOCK_MINUTES = 5;

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
