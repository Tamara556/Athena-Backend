package com.athena.ai.service.impl;

import com.athena.ai.dto.EvaluateInterviewRequest;
import com.athena.ai.dto.GenerateInterviewQuestionsRequest;
import com.athena.ai.model.InterviewEvaluation;
import com.athena.ai.model.InterviewQuestions;
import com.athena.ai.service.AiGenerationService;
import com.athena.ai.service.InterviewAiService;
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
