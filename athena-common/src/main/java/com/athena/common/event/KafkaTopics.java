package com.athena.common.event;

public final class KafkaTopics {

    public static final String TASK_COMPLETED = "athena.task.completed";

    public static final String PLAN_CREATED = "athena.plan.created";

    public static final String STREAK_UPDATED = "athena.streak.updated";

    public static final String BADGE_AWARDED = "athena.badge.awarded";

    private KafkaTopics() {
    }
}
