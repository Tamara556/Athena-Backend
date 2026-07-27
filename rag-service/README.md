# rag-service — Athena Retrieval-Augmented Memory

`rag-service` is Athena's AI memory and retrieval layer. It embeds a learner's own history
(lessons, interviews, roadmap, knowledge graph, achievements and notes) into a pgvector store and
answers questions strictly grounded in that history. It is a dedicated microservice (port `8088`,
database `athena_rag`) and does not extend the existing `ai-service`.

## Position in the platform

```
Angular ─▶ api-gateway (JWT ─▶ X-User-Id) ─▶ rag-service ─▶ athena_rag (Postgres 17 + pgvector, HNSW)
                                                 │
                     Kafka (ingestion) ──────────┤ consumes domain events, keeps embeddings fresh
                     Feign (hydration) ──────────┤ ai-service, progress-service (read existing content)
                     athena-llm (shared) ────────┘ LM Studio chat + embeddings (bge-m3, 1024-d)
```

- Gateway routes: `/rag/**` and `/ai/memory/**` (both JWT-protected; identity is the `X-User-Id`
  header injected by the gateway, never a body field).
- Shared `athena-llm` module provides the provider-agnostic `ChatProvider` / `EmbeddingProvider`
  abstraction with an OpenAI-compatible LM Studio implementation.

## APIs

| Method | Path | Purpose |
|---|---|---|
| POST | `/rag/query` | Grounded RAG answer with citations. Refuses to answer without relevant context. |
| POST | `/rag/search` | Semantic search over the caller's memory (no LLM). |
| POST | `/rag/recommendations/next` | "What should I learn next" — knowledge graph + progress + retrieval + LLM. |
| GET | `/ai/memory/me` | Cross-domain memory profile (source counts, recent memory, KG + progress summary). |
| POST | `/rag/documents` | Ingest a learning material document for the caller. |
| DELETE | `/rag/documents/{id}` | Delete a document and its vectors (ownership enforced). |
| POST | `/rag/reindex/me` | Rebuild the caller's embeddings from stored content. |

## Data model (`athena_rag`)

- `memory_document` — one row per ingested source (userId, sourceType, entityId, learningDomain,
  category, visibility, title, contentHash, status, timestamps). Unique on `(user_id, source_type, entity_id)`.
- `memory_chunk` — chunked content with `embedding vector(1024)` and `metadata jsonb`; HNSW cosine index.
- `rag_query_log` — per-query audit (retrieved count, top score, grounded, model, latency, token usage).

## Ingestion (event-driven)

Consumes `INTERVIEW_EVALUATED`, `ROADMAP_GENERATED`, `LEARNING_SESSION_COMPLETED`,
`KNOWLEDGE_GRAPH_UPDATED`, `BADGE_AWARDED`. Thin events are hydrated to full text via Feign against
existing read endpoints. Re-ingestion is idempotent: unchanged content (by SHA-256 hash) is skipped;
changed content replaces the document's chunks. Publishes `MEMORY_DOCUMENT_INDEXED`.

## Security

Every vector query is filtered by `user_id` at the SQL layer using the gateway-provided identity.
A learner can only ever retrieve their own `PRIVATE` content plus explicitly `GLOBAL` content.
Request bodies cannot override the acting user.

## Configuration

| Property | Default | Notes |
|---|---|---|
| `athena.rag.embedding-dimension` | `1024` | Must match the embedding model and the DDL `vector(N)`. |
| `athena.rag.chunk-max-tokens` / `chunk-overlap-tokens` | `900` / `150` | Chunking window. |
| `athena.rag.retrieval-top-k` / `min-similarity` | `6` / `0.35` | Retrieval and grounding gate. |
| `athena.rag.max-context-tokens` | `6000` | Context assembly budget. |
| `athena.llm.chat.*` / `athena.llm.embedding.*` | LM Studio | Base URL, model, timeouts, batch size. |

Changing the embedding model changes the vector dimension, which requires a new Liquibase changeset
for the `vector(N)` column and a full reindex.

## Build

Requires JDK 26. `./mvnw -pl rag-service -am clean package`. The database uses the
`pgvector/pgvector:pg17` image (wired in `docker-compose.yml` as `rag-db`, host port `5440`).
