package com.athena.ai.dailyjourney.service.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.knowledgegraph.entity.KnowledgeNode;
import com.athena.ai.knowledgegraph.repository.KnowledgeNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WeakNodeFinder {

    private final KnowledgeNodeRepository knowledgeRepository;

    public List<KnowledgeNode> find(UUID userId) {
        return knowledgeRepository.findByUserIdOrderBySkillNameAsc(userId).stream()
                .filter(node -> node.getMasteryPercentage() < AiConstants.WEAKNESS_MASTERY_THRESHOLD)
                .sorted(Comparator.comparingInt(KnowledgeNode::getMasteryPercentage))
                .toList();
    }
}
