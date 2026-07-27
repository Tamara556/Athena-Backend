package com.athena.rag.retrieval.service;

import com.athena.rag.retrieval.domain.RetrievedChunk;

import java.util.List;

public interface RetrievalService {

    List<RetrievedChunk> retrieve(RetrievalRequest request);
}
