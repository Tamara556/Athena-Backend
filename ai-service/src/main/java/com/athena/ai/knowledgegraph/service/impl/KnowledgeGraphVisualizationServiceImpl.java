package com.athena.ai.knowledgegraph.service.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.knowledgegraph.domain.GraphStatus;
import com.athena.ai.knowledgegraph.dto.KnowledgeGraphVisualizationResponse;
import com.athena.ai.knowledgegraph.dto.KnowledgeGraphVisualizationResponse.GraphEdge;
import com.athena.ai.knowledgegraph.dto.KnowledgeGraphVisualizationResponse.GraphNode;
import com.athena.ai.knowledgegraph.dto.KnowledgeGraphVisualizationResponse.GraphSummary;
import com.athena.ai.knowledgegraph.dto.SnapshotSummaryResponse;
import com.athena.ai.knowledgegraph.entity.KnowledgeEdge;
import com.athena.ai.knowledgegraph.entity.KnowledgeGraphSnapshot;
import com.athena.ai.knowledgegraph.entity.KnowledgeNode;
import com.athena.ai.generation.messaging.AiEventPublisher;
import com.athena.ai.knowledgegraph.repository.KnowledgeEdgeRepository;
import com.athena.ai.knowledgegraph.repository.KnowledgeGraphSnapshotRepository;
import com.athena.ai.knowledgegraph.repository.KnowledgeNodeRepository;
import com.athena.ai.knowledgegraph.service.KnowledgeGraphVisualizationService;
import com.athena.common.event.KafkaTopics;
import com.athena.common.event.KnowledgeGraphSnapshotCreatedEvent;
import com.athena.common.event.KnowledgeGraphVisualizationGeneratedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphVisualizationServiceImpl implements KnowledgeGraphVisualizationService {

    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeEdgeRepository edgeRepository;
    private final KnowledgeGraphSnapshotRepository snapshotRepository;
    private final AiEventPublisher events;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    @Override
    @Cacheable(value = AiConstants.CACHE_VISUALIZATION, key = "#userId")
    @Transactional(readOnly = true)
    public KnowledgeGraphVisualizationResponse getVisualization(UUID userId) {
        meterRegistry.counter(AiConstants.METRIC_VIZ_CACHE_MISS).increment();
        return meterRegistry.timer(AiConstants.METRIC_VIZ_GENERATION).record(() -> build(userId));
    }

    @Override
    @Cacheable(value = AiConstants.CACHE_HISTORY, key = "#userId")
    @Transactional(readOnly = true)
    public List<SnapshotSummaryResponse> getHistory(UUID userId) {
        return snapshotRepository.findByUserIdOrderByGeneratedAtDesc(userId).stream()
                .map(s -> new SnapshotSummaryResponse(s.getId(), s.getGeneratedAt(),
                        s.getAverageMastery(), s.getSerializedGraph()))
                .toList();
    }

    @Override
    @CacheEvict(value = {AiConstants.CACHE_VISUALIZATION, AiConstants.CACHE_HISTORY}, key = "#userId")
    @Transactional
    public void onGraphChanged(UUID userId) {
        KnowledgeGraphVisualizationResponse viz = build(userId);
        int avg = viz.summary().averageMastery();

        Optional<KnowledgeGraphSnapshot> last = snapshotRepository.findFirstByUserIdOrderByGeneratedAtDesc(userId);
        boolean significant = last.isEmpty()
                || Math.abs(avg - last.get().getAverageMastery()) >= AiConstants.SNAPSHOT_SIGNIFICANT_DELTA;
        if (significant) {
            KnowledgeGraphSnapshot snapshot = snapshotRepository.save(
                    new KnowledgeGraphSnapshot(userId, avg, objectMapper.writeValueAsString(viz)));
            meterRegistry.counter(AiConstants.METRIC_SNAPSHOTS_CREATED).increment();
            events.publish(KafkaTopics.KNOWLEDGE_GRAPH_SNAPSHOT_CREATED, userId,
                    new KnowledgeGraphSnapshotCreatedEvent(userId, snapshot.getId(), avg, Instant.now(clock)));
            log.info("Created knowledge snapshot userId={} avgMastery={}", userId, avg);
        }
        events.publish(KafkaTopics.KNOWLEDGE_GRAPH_VISUALIZATION_GENERATED, userId,
                new KnowledgeGraphVisualizationGeneratedEvent(
                        userId, viz.domain(), viz.summary().totalSkills(), avg, Instant.now(clock)));
    }

    private KnowledgeGraphVisualizationResponse build(UUID userId) {
        List<KnowledgeNode> nodes = nodeRepository.findByUserIdOrderBySkillNameAsc(userId);
        List<KnowledgeEdge> edges = edgeRepository.findByUserId(userId);

        String domain = nodes.stream().map(KnowledgeNode::getDomain)
                .filter(d -> d != null && !d.isBlank()).findFirst().orElse("General");

        List<GraphNode> graphNodes = nodes.stream().map(this::toGraphNode).toList();
        List<GraphEdge> graphEdges = edges.stream()
                .map(e -> new GraphEdge(slug(e.getSourceSkill()), slug(e.getTargetSkill()),
                        e.getRelationshipType().name()))
                .toList();

        GraphSummary summary = buildSummary(nodes);
        List<String> insights = buildInsights(nodes, edges);

        return new KnowledgeGraphVisualizationResponse(
                domain, Instant.now(clock), graphNodes, graphEdges, summary, insights);
    }

    private GraphNode toGraphNode(KnowledgeNode n) {
        int confidence = (int) Math.round(n.getConfidenceScore() * 100);
        String category = n.getCategory() == null || n.getCategory().isBlank() ? "CORE" : n.getCategory();
        return new GraphNode(slug(n.getSkillName()), n.getSkillName(), n.getMasteryPercentage(),
                confidence, GraphStatus.fromMastery(n.getMasteryPercentage()).name(), category);
    }

    private GraphSummary buildSummary(List<KnowledgeNode> nodes) {
        if (nodes.isEmpty()) {
            return new GraphSummary(List.of(), List.of(), 0, 0);
        }
        List<KnowledgeNode> byMastery = new ArrayList<>(nodes);
        byMastery.sort(Comparator.comparingInt(KnowledgeNode::getMasteryPercentage));
        String weakest = byMastery.getFirst().getSkillName();
        String strongest = byMastery.getLast().getSkillName();
        int avg = (int) Math.round(nodes.stream().mapToInt(KnowledgeNode::getMasteryPercentage).average().orElse(0));
        return new GraphSummary(List.of(strongest), List.of(weakest), avg, nodes.size());
    }

    private List<String> buildInsights(List<KnowledgeNode> nodes, List<KnowledgeEdge> edges) {
        if (nodes.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> masteryBySkill = new HashMap<>();
        nodes.forEach(n -> masteryBySkill.put(n.getSkillName(), n.getMasteryPercentage()));

        KnowledgeNode strongest = nodes.stream()
                .max(Comparator.comparingInt(KnowledgeNode::getMasteryPercentage)).orElseThrow();
        KnowledgeNode weakest = nodes.stream()
                .min(Comparator.comparingInt(KnowledgeNode::getMasteryPercentage)).orElseThrow();

        List<String> insights = new ArrayList<>();
        insights.add("Your strongest area is %s.".formatted(strongest.getSkillName()));
        insights.add("%s remains your weakest skill.".formatted(weakest.getSkillName()));

        edges.stream()
                .filter(e -> masteryBySkill.getOrDefault(e.getTargetSkill(), 100) < 50)
                .filter(e -> masteryBySkill.getOrDefault(e.getSourceSkill(), 0)
                        > masteryBySkill.getOrDefault(e.getTargetSkill(), 0))
                .findFirst()
                .ifPresent(e -> insights.add(
                        "Improving %s knowledge may accelerate your progress in %s."
                                .formatted(e.getSourceSkill(), e.getTargetSkill())));

        return insights.size() > 3 ? insights.subList(0, 3) : insights;
    }

    private String slug(String skillName) {
        return skillName.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }
}
