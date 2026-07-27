package com.athena.ai.interview.service.impl;

import com.athena.ai.interview.dto.EvaluateInterviewRequest;
import com.athena.ai.interview.dto.GenerateInterviewQuestionsRequest;
import com.athena.ai.generation.model.InterviewEvaluation;
import com.athena.ai.generation.model.InterviewQuestions;
import com.athena.ai.generation.service.AiGenerationService;
import com.athena.ai.interview.service.InterviewAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InterviewAiServiceImpl implements InterviewAiService {

    private final AiGenerationService generation;

    @Override
    public InterviewQuestions generateQuestions(GenerateInterviewQuestionsRequest request) {
        return generation.generateInterviewQuestions(request.userId(), request.domain(), request.level());
    }

    @Override
    public InterviewEvaluation evaluate(EvaluateInterviewRequest request) {
        StringBuilder qa = new StringBuilder();
        for (EvaluateInterviewRequest.QnA item : request.answers()) {
            qa.append("- ").append(item.question()).append(" -> ").append(item.answer()).append('\n');
        }
        return generation.evaluateInterview(request.userId(), request.domain(), qa.toString());
    }
}
