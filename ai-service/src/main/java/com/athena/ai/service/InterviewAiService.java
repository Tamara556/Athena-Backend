package com.athena.ai.service;

import com.athena.ai.dto.EvaluateInterviewRequest;
import com.athena.ai.dto.GenerateInterviewQuestionsRequest;
import com.athena.ai.model.InterviewEvaluation;
import com.athena.ai.model.InterviewQuestions;

public interface InterviewAiService {

    InterviewQuestions generateQuestions(GenerateInterviewQuestionsRequest request);

    InterviewEvaluation evaluate(EvaluateInterviewRequest request);
}
