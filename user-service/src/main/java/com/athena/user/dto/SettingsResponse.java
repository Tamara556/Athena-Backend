package com.athena.user.dto;

public record SettingsResponse(Learning learning, Experience experience, Notifications notifications, Privacy privacy) {

    public record Learning(String availability, String difficulty, String style) {
    }

    public record Experience(String tone, boolean motivational, boolean reflection, boolean adaptive) {
    }

    public record Notifications(boolean dailyReminder, boolean weeklySummary, boolean interviewReminders,
                                boolean milestones) {
    }

    public record Privacy(boolean personalize, boolean shareAnon) {
    }
}
