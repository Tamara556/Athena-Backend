package com.athena.ai.knowledgegraph.dto;

import com.athena.ai.constants.AiConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateKnowledgeRequest(

        @NotEmpty(message = AiConstants.NODES_NOT_EMPTY)
        @Valid
        List<NodeUpsert> nodes
) {

    public record NodeUpsert(
            @NotBlank(message = AiConstants.SKILL_NAME_REQUIRED) String skillName,
            String domain,
            @Min(value = 0, message = AiConstants.MASTERY_PERCENTAGE_MIN_MESSAGE)
            @Max(value = 100, message = AiConstants.MASTERY_PERCENTAGE_MAX_MESSAGE)
            int masteryPercentage,
            @Min(value = 0, message = AiConstants.CONFIDENCE_SCORE_MIN_MESSAGE)
            @Max(value = 1, message = AiConstants.CONFIDENCE_SCORE_MAX_MESSAGE)
            double confidenceScore
    ) {
    }
}
