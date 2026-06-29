package com.athena.common.event;

public final class KafkaTopics {

    public static final String TASK_COMPLETED = "athena.task.completed";

    public static final String PLAN_CREATED = "athena.plan.created";

    public static final String STREAK_UPDATED = "athena.streak.updated";

    public static final String BADGE_AWARDED = "athena.badge.awarded";

    public static final String ONBOARDING_STARTED = "athena.onboarding.started";
    public static final String GOAL_DISCOVERED = "athena.goal.discovered";
    public static final String ASSESSMENT_COMPLETED = "athena.assessment.completed";
    public static final String GOAL_ANALYZED = "athena.goal.analyzed";
    public static final String ROADMAP_GENERATED = "athena.roadmap.generated";
    public static final String DAILY_PLAN_GENERATED = "athena.dailyplan.generated";
    public static final String RECOMMENDATION_GENERATED = "athena.recommendation.generated";
    public static final String KNOWLEDGE_GRAPH_UPDATED = "athena.knowledge.updated";
    public static final String INTERVIEW_STARTED = "athena.interview.started";
    public static final String INTERVIEW_COMPLETED = "athena.interview.completed";
    public static final String INTERVIEW_EVALUATED = "athena.interview.evaluated";

    public static final String USER_REGISTERED = "athena.user.registered";
    public static final String BADGE_SUGGESTION_GENERATED = "athena.badge.suggestion.generated";
    public static final String KNOWLEDGE_GRAPH_VISUALIZATION_GENERATED = "athena.knowledge.visualization.generated";
    public static final String KNOWLEDGE_GRAPH_SNAPSHOT_CREATED = "athena.knowledge.snapshot.created";

    public static final String LEARNING_SESSION_GENERATED = "athena.learning.session.generated";
    public static final String LEARNING_SESSION_STARTED = "athena.learning.session.started";
    public static final String LEARNING_SESSION_COMPLETED = "athena.learning.session.completed";
    public static final String NODE_BUFFER_REFILLED = "athena.learning.node.buffer.refilled";

    private KafkaTopics() {
    }
}
