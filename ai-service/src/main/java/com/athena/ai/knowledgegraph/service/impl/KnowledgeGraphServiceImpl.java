package com.athena.ai.knowledgegraph.service.impl;

import com.athena.ai.knowledgegraph.domain.RelationshipType;
import com.athena.ai.knowledgegraph.dto.KnowledgeNodeResponse;
import com.athena.ai.knowledgegraph.dto.UpdateKnowledgeRequest;
import com.athena.ai.knowledgegraph.entity.KnowledgeEdge;
import com.athena.ai.knowledgegraph.entity.KnowledgeNode;
import com.athena.ai.generation.messaging.AiEventPublisher;
import com.athena.ai.knowledgegraph.repository.KnowledgeEdgeRepository;
import com.athena.ai.knowledgegraph.repository.KnowledgeNodeRepository;
import com.athena.ai.knowledgegraph.service.KnowledgeGraphService;
import com.athena.ai.knowledgegraph.service.KnowledgeGraphVisualizationService;
import com.athena.common.event.KafkaTopics;
import com.athena.common.event.KnowledgeGraphUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private static final int SEED_MASTERY = 20;
    private static final double SEED_CONFIDENCE = 0.3;
    private static final int WEAKNESS_MASTERY = 35;
    private static final double WEAKNESS_CONFIDENCE = 0.4;
    private static final String CATEGORY_ADVANCED = "ADVANCED";

    private final KnowledgeNodeRepository repository;
    private final KnowledgeEdgeRepository edgeRepository;
    private final AiEventPublisher events;
    private final KnowledgeGraphVisualizationService visualization;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeNodeResponse> getForUser(UUID userId) {
        return repository.findByUserIdOrderBySkillNameAsc(userId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public List<KnowledgeNodeResponse> applyUpdates(UUID userId, UpdateKnowledgeRequest request) {
        request.nodes().forEach(n ->
                upsert(userId, n.skillName(), n.domain(), n.masteryPercentage(), n.confidenceScore(), null));
        afterChange(userId);
        return getForUser(userId);
    }

    @Override
    @Transactional
    public void seedSkills(UUID userId, String domain, List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return;
        }
        skills.forEach(skill -> repository.findByUserIdAndSkillNameIgnoreCase(userId, skill)
                .orElseGet(() -> repository.save(
                        new KnowledgeNode(userId, skill, domain, SEED_MASTERY, SEED_CONFIDENCE))));
        for (int i = 0; i < skills.size() - 1; i++) {
            createEdgeIfAbsent(userId, skills.get(i), skills.get(i + 1), RelationshipType.PREREQUISITE);
        }
        afterChange(userId);
        log.info("Seeded {} knowledge nodes userId={}", skills.size(), userId);
    }

    @Override
    @Transactional
    public void recordWeaknesses(UUID userId, String domain, List<String> weaknesses) {
        if (weaknesses == null || weaknesses.isEmpty()) {
            return;
        }
        String strongest = repository.findByUserIdOrderBySkillNameAsc(userId).stream()
                .max(Comparator.comparingInt(KnowledgeNode::getMasteryPercentage))
                .map(KnowledgeNode::getSkillName)
                .orElse(null);

        weaknesses.forEach(skill -> {
            upsert(userId, skill, domain, WEAKNESS_MASTERY, WEAKNESS_CONFIDENCE, CATEGORY_ADVANCED);
            if (strongest != null && !strongest.equalsIgnoreCase(skill)) {
                createEdgeIfAbsent(userId, strongest, skill, RelationshipType.SUPPORTS);
            }
        });
        afterChange(userId);
        log.info("Recorded {} weakness nodes userId={}", weaknesses.size(), userId);
    }

    private void upsert(UUID userId, String skill, String domain, int mastery, double confidence, String category) {
        KnowledgeNode node = repository.findByUserIdAndSkillNameIgnoreCase(userId, skill)
                .orElseGet(() -> new KnowledgeNode(userId, skill, domain, mastery, confidence));
        node.setMasteryPercentage(mastery);
        node.setConfidenceScore(confidence);
        if (domain != null && !domain.isBlank()) {
            node.setDomain(domain);
        }
        if (category != null) {
            node.setCategory(category);
        }
        repository.save(node);
    }

    private void createEdgeIfAbsent(UUID userId, String source, String target, RelationshipType type) {
        if (source.equalsIgnoreCase(target)) {
            return;
        }
        if (!edgeRepository.existsByUserIdAndSourceSkillAndTargetSkillAndRelationshipType(userId, source, target, type)) {
            edgeRepository.save(new KnowledgeEdge(userId, source, target, type));
        }
    }

    private void afterChange(UUID userId) {
        events.publish(KafkaTopics.KNOWLEDGE_GRAPH_UPDATED, userId,
                new KnowledgeGraphUpdatedEvent(userId, Instant.now(clock)));
        visualization.onGraphChanged(userId);
    }

    private KnowledgeNodeResponse toResponse(KnowledgeNode node) {
        return new KnowledgeNodeResponse(node.getSkillName(), node.getDomain(),
                node.getMasteryPercentage(), node.getConfidenceScore(), node.getUpdatedAt());
    }
}
