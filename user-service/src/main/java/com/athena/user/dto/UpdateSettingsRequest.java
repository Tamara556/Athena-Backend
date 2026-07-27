package com.athena.user.dto;

import com.athena.user.constants.UserConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSettingsRequest(
        @NotNull @Valid Learning learning,
        @NotNull @Valid Experience experience,
        @NotNull @Valid Notifications notifications,
        @NotNull @Valid Privacy privacy
) {

    public record Learning(
            @NotBlank @Size(max = UserConstants.SETTING_VALUE_MAX_LENGTH) String availability,
            @NotBlank @Size(max = UserConstants.SETTING_VALUE_MAX_LENGTH) String difficulty,
            @NotBlank @Size(max = UserConstants.SETTING_VALUE_MAX_LENGTH) String style) {
    }

    public record Experience(
            @NotBlank @Size(max = UserConstants.SETTING_VALUE_MAX_LENGTH) String tone,
            boolean motivational, boolean reflection, boolean adaptive) {
    }

    public record Notifications(boolean dailyReminder, boolean weeklySummary, boolean interviewReminders,
                                boolean milestones) {
    }

    public record Privacy(boolean personalize, boolean shareAnon) {
    }
}
