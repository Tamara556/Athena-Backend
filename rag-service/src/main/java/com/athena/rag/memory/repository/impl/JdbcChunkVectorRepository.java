package com.athena.rag.memory.repository.impl;

import com.athena.rag.memory.repository.ChunkInsert;
import com.athena.rag.memory.repository.ChunkVectorRepository;
import com.athena.rag.memory.repository.VectorFormat;
import com.athena.rag.memory.repository.VectorMatch;
import com.athena.rag.memory.repository.VectorSearchQuery;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcChunkVectorRepository implements ChunkVectorRepository {

    private static final String INSERT_SQL = """
            INSERT INTO memory_chunk
                (id, document_id, user_id, chunk_index, content, token_count, embedding, metadata, created_at)
            VALUES
                (:id, :documentId, :userId, :chunkIndex, :content, :tokenCount,
                 CAST(:embedding AS vector), CAST(:metadata AS jsonb), :createdAt)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcChunkVectorRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insertBatch(List<ChunkInsert> chunks) {
        if (chunks.isEmpty()) {
            return;
        }
        SqlParameterSource[] batch = chunks.stream().map(this::toParams).toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate(INSERT_SQL, batch);
    }

    @Override
    public void deleteByDocumentId(UUID documentId) {
        jdbc.update("DELETE FROM memory_chunk WHERE document_id = :documentId",
                new MapSqlParameterSource("documentId", documentId));
    }

    @Override
    public List<VectorMatch> search(VectorSearchQuery query) {
        String vector = VectorFormat.toText(query.queryVector());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("q", vector)
                .addValue("userId", query.userId())
                .addValue("limit", query.limit())
                .addValue("offset", query.offset());

        StringBuilder sql = new StringBuilder("""
                SELECT c.id AS chunk_id, c.document_id, c.user_id, d.source_type, d.entity_id,
                       d.learning_domain, d.category, d.title, c.content,
                       1 - (c.embedding <=> CAST(:q AS vector)) AS score
                FROM memory_chunk c
                JOIN memory_document d ON d.id = c.document_id
                WHERE (c.user_id = :userId
                """);
        if (query.includeGlobal()) {
            sql.append(" OR d.visibility = 'GLOBAL'");
        }
        sql.append(")");

        if (query.sourceTypes() != null && !query.sourceTypes().isEmpty()) {
            sql.append(" AND d.source_type IN (:sourceTypes)");
            params.addValue("sourceTypes", query.sourceTypes());
        }
        if (query.learningDomain() != null && !query.learningDomain().isBlank()) {
            sql.append(" AND d.learning_domain = :domain");
            params.addValue("domain", query.learningDomain());
        }
        sql.append(" ORDER BY c.embedding <=> CAST(:q AS vector) LIMIT :limit OFFSET :offset");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> new VectorMatch(
                rs.getObject("chunk_id", UUID.class),
                rs.getObject("document_id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("source_type"),
                rs.getObject("entity_id", UUID.class),
                rs.getString("learning_domain"),
                rs.getString("category"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getDouble("score")));
    }

    private SqlParameterSource toParams(ChunkInsert chunk) {
        return new MapSqlParameterSource()
                .addValue("id", chunk.id())
                .addValue("documentId", chunk.documentId())
                .addValue("userId", chunk.userId())
                .addValue("chunkIndex", chunk.chunkIndex())
                .addValue("content", chunk.content())
                .addValue("tokenCount", chunk.tokenCount())
                .addValue("embedding", VectorFormat.toText(chunk.embedding()))
                .addValue("metadata", chunk.metadataJson())
                .addValue("createdAt", Timestamp.from(chunk.createdAt()));
    }
}
