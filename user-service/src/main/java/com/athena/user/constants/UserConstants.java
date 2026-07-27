package com.athena.user.constants;

public final class UserConstants {

    private UserConstants() {
    }

    // Field limits
    public static final int NAME_MAX_LENGTH = 120;
    public static final int AGE_MIN = 5;
    public static final int AGE_MAX = 120;
    public static final int GOAL_MAX_LENGTH = 500;
    public static final String DAILY_STUDY_HOURS_MIN = "0.0";
    public static final String DAILY_STUDY_HOURS_MAX = "24.0";

    // Validation messages
    public static final String USER_ID_REQUIRED = "userId is required";
    public static final String NAME_REQUIRED = "name is required";
    public static final String NAME_MAX_LENGTH_MESSAGE =
            "name must be at most " + NAME_MAX_LENGTH + " characters";
    public static final String AGE_MIN_MESSAGE = "age must be at least " + AGE_MIN;
    public static final String AGE_MAX_MESSAGE = "age must be at most " + AGE_MAX;
    public static final String GOAL_REQUIRED = "goal is required";
    public static final String GOAL_MAX_LENGTH_MESSAGE =
            "goal must be at most " + GOAL_MAX_LENGTH + " characters";
    public static final String DAILY_STUDY_HOURS_NEGATIVE = "dailyStudyHours cannot be negative";
    public static final String DAILY_STUDY_HOURS_MAX_MESSAGE = "dailyStudyHours cannot exceed 24";

    // Service messages
    public static final String RESOURCE_NAME = "User profile";
    public static final String PROFILE_ALREADY_EXISTS = "A profile already exists for user %s";

    // Settings
    public static final int SETTING_VALUE_MAX_LENGTH = 20;
    public static final String DEFAULT_AVAILABILITY = "2h";
    public static final String DEFAULT_DIFFICULTY = "balanced";
    public static final String DEFAULT_STYLE = "practical";
    public static final String DEFAULT_TONE = "encouraging";
}
