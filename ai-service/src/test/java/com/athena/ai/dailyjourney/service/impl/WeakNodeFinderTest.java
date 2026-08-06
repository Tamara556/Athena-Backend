package com.athena.ai.dailyjourney.service.impl;

import com.athena.ai.knowledgegraph.entity.KnowledgeNode;
import com.athena.ai.knowledgegraph.repository.KnowledgeNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeakNodeFinderTest {

    @Mock
    private KnowledgeNodeRepository knowledgeRepository;

    @InjectMocks
    private WeakNodeFinder finder;

    private final UUID userId = UUID.randomUUID();

    @Test
    void keepsOnlyBelowThresholdNodesSortedByAscendingMastery() {
        when(knowledgeRepository.findByUserIdOrderBySkillNameAsc(userId)).thenReturn(List.of(
                node("Strong", 90),
                node("Middling", 50),
                node("AtThreshold", 70),
                node("Weakest", 30)));

        List<KnowledgeNode> result = finder.find(userId);

        // < 70 only (70 excluded), ascending by mastery.
        assertThat(result).extracting(KnowledgeNode::getMasteryPercentage).containsExactly(30, 50);
        assertThat(result).extracting(KnowledgeNode::getSkillName).containsExactly("Weakest", "Middling");
    }

    @Test
    void returnsEmptyWhenNoWeakNodes() {
        when(knowledgeRepository.findByUserIdOrderBySkillNameAsc(userId))
                .thenReturn(List.of(node("Strong", 85), node("AtThreshold", 70)));

        assertThat(finder.find(userId)).isEmpty();
    }

    private KnowledgeNode node(String skill, int mastery) {
        return new KnowledgeNode(userId, skill, "domain", mastery, 0.5);
    }
}
