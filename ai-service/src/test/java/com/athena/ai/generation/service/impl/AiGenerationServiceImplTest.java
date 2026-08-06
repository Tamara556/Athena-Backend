package com.athena.ai.generation.service.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.generation.model.GoalAnalysis;
import com.athena.ai.generation.model.RoadmapContent;
import com.athena.ai.llm.LlmService;
import com.athena.ai.llm.PromptTemplateService;
import com.athena.llm.model.ResponseFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiGenerationServiceImplTest {

    @Mock
    private PromptTemplateService prompts;
    @Mock
    private LlmService llm;

    private AiGenerationServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    private AiGenerationServiceImpl service() {
        if (service == null) {
            service = new AiGenerationServiceImpl(prompts, llm);
        }
        return service;
    }

    @Test
    void generateRoadmapAssignsProgressStatusesToPhases() {
        when(prompts.render(eq(AiConstants.TPL_SYSTEM), any())).thenReturn("SYS");
        when(prompts.render(eq(AiConstants.TPL_ROADMAP), any())).thenReturn("USER");
        RoadmapContent raw = new RoadmapContent(List.of(
                phase("A"), phase("B"), phase("C")));
        when(llm.generateJson(eq(userId), eq(AiConstants.PROMPT_ROADMAP), eq("SYS"), eq("USER"),
                eq(RoadmapContent.class), any(ResponseFormat.class))).thenReturn(raw);

        RoadmapContent result = service().generateRoadmap(userId, "Learn SQL", analysis(List.of()));

        assertThat(result.phases()).extracting(RoadmapContent.Phase::status)
                .containsExactly("CURRENT", "AVAILABLE", "LOCKED");
    }

    @Test
    void generateRoadmapJoinsPrerequisitesOrEmitsNone() {
        when(prompts.render(eq(AiConstants.TPL_SYSTEM), any())).thenReturn("SYS");
        when(prompts.render(eq(AiConstants.TPL_ROADMAP), any())).thenReturn("USER");
        when(llm.generateJson(any(), any(), any(), any(), eq(RoadmapContent.class), any(ResponseFormat.class)))
                .thenReturn(new RoadmapContent(List.of(phase("A"))));

        service().generateRoadmap(userId, "Learn SQL", analysis(List.of()));

        ArgumentCaptor<Map<String, String>> vars = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(prompts).render(eq(AiConstants.TPL_ROADMAP), vars.capture());
        assertThat(vars.getValue()).containsEntry("prerequisites", "none");
    }

    @Test
    void generateRoadmapPassesJoinedPrerequisites() {
        when(prompts.render(eq(AiConstants.TPL_SYSTEM), any())).thenReturn("SYS");
        when(prompts.render(eq(AiConstants.TPL_ROADMAP), any())).thenReturn("USER");
        when(llm.generateJson(any(), any(), any(), any(), eq(RoadmapContent.class), any(ResponseFormat.class)))
                .thenReturn(new RoadmapContent(List.of(phase("A"))));

        service().generateRoadmap(userId, "Learn SQL", analysis(List.of("algebra", "logic")));

        ArgumentCaptor<Map<String, String>> vars = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(prompts).render(eq(AiConstants.TPL_ROADMAP), vars.capture());
        assertThat(vars.getValue()).containsEntry("prerequisites", "algebra, logic");
    }

    @Test
    void generateRoadmapToleratesNullPhases() {
        when(prompts.render(any(), any())).thenReturn("X");
        when(llm.generateJson(any(), any(), any(), any(), eq(RoadmapContent.class), any(ResponseFormat.class)))
                .thenReturn(new RoadmapContent(null));

        RoadmapContent result = service().generateRoadmap(userId, "Learn SQL", analysis(List.of()));

        assertThat(result.phases()).isNull();
    }

    private static RoadmapContent.Phase phase(String name) {
        return new RoadmapContent.Phase(name, "desc", 3, List.of("obj"), null);
    }

    private static GoalAnalysis analysis(List<String> prerequisites) {
        return new GoalAnalysis("databases", "BEGINNER", 6, 2.0, prerequisites);
    }
}
