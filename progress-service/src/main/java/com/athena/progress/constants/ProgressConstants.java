package com.athena.progress.constants;

public final class ProgressConstants {

    public static final int TASKS_COMPLETED_MIN = 0;
    public static final int TASKS_COMPLETED_MAX = 1000;
    public static final int MINUTES_SPENT_MIN = 0;
    public static final int MAX_MINUTES_PER_DAY = 1440;
    public static final int WEEK_DAYS = 7;

    public static final String USER_ID_REQUIRED = "userId is required";
    public static final String TASKS_COMPLETED_NEGATIVE = "tasksCompleted cannot be negative";
    public static final String TASKS_COMPLETED_MAX_MESSAGE = "tasksCompleted per update is unrealistically high";
    public static final String MINUTES_SPENT_NEGATIVE = "minutesSpent cannot be negative";
    public static final String MINUTES_SPENT_MAX_MESSAGE =
            "minutesSpent cannot exceed one day (" + MAX_MINUTES_PER_DAY + ")";

    public static final String RESOURCE_NAME = "Progress";
    public static final String USER_NOT_FOUND_FOR_PROGRESS = "Cannot track progress: user %s does not exist";
}
