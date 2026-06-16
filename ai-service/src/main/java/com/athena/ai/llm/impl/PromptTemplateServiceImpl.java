package com.athena.ai.llm.impl;

import com.athena.ai.llm.PromptTemplateService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Override
    public String render(String templateName, Map<String, String> variables) {
        String template = cache.computeIfAbsent(templateName, this::load);
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("{{" + entry.getKey() + "}}", value);
        }
        return result;
    }

    private String load(String name) {
        ClassPathResource resource = new ClassPathResource("prompts/" + name + ".txt");
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Missing prompt template: " + name, ex);
        }
    }
}
