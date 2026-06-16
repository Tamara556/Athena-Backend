package com.athena.interview.dto;

import com.athena.interview.constants.InterviewConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SubmitInterviewRequest(

        @NotEmpty(message = InterviewConstants.ANSWERS_NOT_EMPTY)
        @Valid
        List<AnswerInput> answers
) {

    public record AnswerInput(
            @NotNull(message = InterviewConstants.QUESTION_ID_REQUIRED) UUID questionId,
            @NotBlank(message = InterviewConstants.ANSWER_REQUIRED) String answer
    ) {
    }
}
