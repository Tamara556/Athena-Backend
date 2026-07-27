package com.athena.rag.rag.service;

import com.athena.rag.rag.dto.RagAnswerResponse;
import com.athena.rag.rag.dto.RagQueryRequest;

import java.util.UUID;

public interface RagQueryService {

    RagAnswerResponse answer(UUID userId, RagQueryRequest request);
}
