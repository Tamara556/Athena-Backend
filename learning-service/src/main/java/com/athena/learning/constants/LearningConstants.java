package com.athena.learning.constants;

public final class LearningConstants {

    private LearningConstants() {
    }

    public static final int TITLE_MAX_LENGTH = 150;
    public static final int DESCRIPTION_MAX_LENGTH = 2000;
    public static final int ESTIMATED_MINUTES_MIN = 1;
    public static final int MAX_MINUTES_PER_DAY = 1440;

    public static final String PLAN_ID_REQUIRED = "planId is required";
    public static final String TITLE_REQUIRED = "title is required";
    public static final String TITLE_MAX_LENGTH_MESSAGE =
            "title must be at most " + TITLE_MAX_LENGTH + " characters";
    public static final String DESCRIPTION_MAX_LENGTH_MESSAGE =
            "description must be at most " + DESCRIPTION_MAX_LENGTH + " characters";
    public static final String TASK_TYPE_REQUIRED = "taskType is required";
    public static final String ESTIMATED_MINUTES_MIN_MESSAGE =
            "estimatedMinutes must be at least " + ESTIMATED_MINUTES_MIN;
    public static final String ESTIMATED_MINUTES_MAX_MESSAGE =
            "estimatedMinutes cannot exceed one day (" + MAX_MINUTES_PER_DAY + ")";
    public static final String TASK_ID_REQUIRED = "taskId is required";
    public static final String SESSION_ID_REQUIRED = "sessionId is required";

    public static final String LEARNING_PLAN_RESOURCE = "Learning plan";
    public static final String LEARNING_TASK_RESOURCE = "Learning task";
    public static final String LEARNING_SESSION_RESOURCE = "Learning session";
}
