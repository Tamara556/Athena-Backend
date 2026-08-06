package com.athena.rag.memory.repository;

import com.athena.rag.memory.domain.SourceType;
import com.athena.rag.memory.domain.Visibility;
import com.athena.rag.memory.entity.MemoryDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.kafka.listener.auto-startup=false",
                "spring.cache.type=none",
                "spring.jpa.hibernate.ddl-auto=validate"
        })
@Testcontainers
class JdbcChunkVectorRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> PGVECTOR = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"))
            .withStartupTimeout(Duration.ofSeconds(120));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PGVECTOR::getJdbcUrl);
        registry.add("spring.datasource.username", PGVECTOR::getUsername);
        registry.add("spring.datasource.password", PGVECTOR::getPassword);
    }

    @Autowired
    private ChunkVectorRepository vectorRepository;
    @Autowired
    private MemoryDocumentRepository documentRepository;

    @Test
    void searchRanksChunksByCosineSimilarityToTheQueryVector() {
        UUID userId = UUID.randomUUID();
        UUID docId = insertDocument(userId, Visibility.PRIVATE, "Databases");

        UUID exact = UUID.randomUUID();
        UUID near = UUID.randomUUID();
        UUID orthogonal = UUID.randomUUID();
        vectorRepository.insertBatch(List.of(
                chunk(exact, docId, userId, 0, "exact match", unit(0)),
                chunk(near, docId, userId, 1, "near match", biased(0.9f, 0.1f)),
                chunk(orthogonal, docId, userId, 2, "unrelated", unit(1))));

        List<VectorMatch> matches = vectorRepository.search(new VectorSearchQuery(
                userId, unit(0), null, null, false, 10, 0));

        assertThat(matches).extracting(VectorMatch::chunkId).containsExactly(exact, near, orthogonal);
        assertThat(matches.getFirst().score()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-4));
        assertThat(matches.get(2).score()).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-4));
    }

    @Test
    void includeGlobalControlsCrossUserVisibility() {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        UUID ownDoc = insertDocument(owner, Visibility.PRIVATE, "Owned");
        UUID globalDoc = insertDocument(stranger, Visibility.GLOBAL, "Shared");

        UUID ownChunk = UUID.randomUUID();
        UUID globalChunk = UUID.randomUUID();
        vectorRepository.insertBatch(List.of(chunk(ownChunk, ownDoc, owner, 0, "own", unit(0))));
        vectorRepository.insertBatch(List.of(chunk(globalChunk, globalDoc, stranger, 0, "global", unit(0))));

        List<VectorMatch> withoutGlobal = vectorRepository.search(new VectorSearchQuery(
                owner, unit(0), null, null, false, 10, 0));
        assertThat(withoutGlobal).extracting(VectorMatch::chunkId).containsExactly(ownChunk);

        List<VectorMatch> withGlobal = vectorRepository.search(new VectorSearchQuery(
                owner, unit(0), null, null, true, 10, 0));
        assertThat(withGlobal).extracting(VectorMatch::chunkId).containsExactlyInAnyOrder(ownChunk, globalChunk);
    }

    @Test
    void filtersBySourceType() {
        UUID userId = UUID.randomUUID();
        UUID lessonDoc = insertDocument(userId, Visibility.PRIVATE, SourceType.LEARNING_SESSION, "Lesson");
        UUID interviewDoc = insertDocument(userId, Visibility.PRIVATE, SourceType.INTERVIEW, "Interview");
        UUID lessonChunk = UUID.randomUUID();
        vectorRepository.insertBatch(List.of(chunk(lessonChunk, lessonDoc, userId, 0, "lesson", unit(0))));
        vectorRepository.insertBatch(List.of(chunk(UUID.randomUUID(), interviewDoc, userId, 0, "interview", unit(0))));

        List<VectorMatch> lessonsOnly = vectorRepository.search(new VectorSearchQuery(
                userId, unit(0), List.of("LEARNING_SESSION"), null, false, 10, 0));

        assertThat(lessonsOnly).extracting(VectorMatch::chunkId).containsExactly(lessonChunk);
        assertThat(lessonsOnly.getFirst().sourceType()).isEqualTo("LEARNING_SESSION");
    }

    @Test
    void deleteByDocumentIdRemovesAllItsChunks() {
        UUID userId = UUID.randomUUID();
        UUID docId = insertDocument(userId, Visibility.PRIVATE, "Doomed");
        vectorRepository.insertBatch(List.of(
                chunk(UUID.randomUUID(), docId, userId, 0, "a", unit(0)),
                chunk(UUID.randomUUID(), docId, userId, 1, "b", unit(0))));

        vectorRepository.deleteByDocumentId(docId);

        assertThat(vectorRepository.search(new VectorSearchQuery(userId, unit(0), null, null, false, 10, 0)))
                .isEmpty();
    }

    private UUID insertDocument(UUID userId, Visibility visibility, String title) {
        return insertDocument(userId, visibility, SourceType.LEARNING_SESSION, title);
    }

    private UUID insertDocument(UUID userId, Visibility visibility, SourceType sourceType, String title) {
        MemoryDocument document = new MemoryDocument(userId, sourceType, UUID.randomUUID(),
                "Databases", "core", visibility, title, UUID.randomUUID().toString());
        return documentRepository.saveAndFlush(document).getId();
    }

    private static ChunkInsert chunk(UUID id, UUID documentId, UUID userId, int index, String content, float[] embedding) {
        return new ChunkInsert(id, documentId, userId, index, content, 3, embedding, "{}", Instant.now());
    }

    private static float[] unit(int index) {
        float[] vector = new float[1024];
        vector[index] = 1.0f;
        return vector;
    }

    private static float[] biased(float first, float second) {
        float[] vector = new float[1024];
        vector[0] = first;
        vector[1] = second;
        return vector;
    }
}
