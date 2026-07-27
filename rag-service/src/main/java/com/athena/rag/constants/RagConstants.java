package com.athena.rag.constants;

public final class RagConstants {

    public static final String RESOURCE_DOCUMENT = "MemoryDocument";

    public static final String METRIC_EMBED_LATENCY = "athena.rag.embedding.latency";
    public static final String METRIC_EMBED_FAILURES = "athena.rag.embedding.failures";
    public static final String METRIC_RETRIEVAL_LATENCY = "athena.rag.retrieval.latency";
    public static final String METRIC_RETRIEVAL_FAILURES = "athena.rag.retrieval.failures";
    public static final String METRIC_QUERY_LATENCY = "athena.rag.query.latency";
    public static final String METRIC_QUERY_UNGROUNDED = "athena.rag.query.ungrounded";
    public static final String METRIC_LLM_FAILURES = "athena.rag.llm.failures";
    public static final String METRIC_RETRIEVED_DOCS = "athena.rag.retrieval.documents";
    public static final String METRIC_TOP_SCORE = "athena.rag.retrieval.top.score";

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_UNGROUNDED = "UNGROUNDED";
    public static final String STATUS_FAILED = "FAILED";

    private RagConstants() {
    }
}
