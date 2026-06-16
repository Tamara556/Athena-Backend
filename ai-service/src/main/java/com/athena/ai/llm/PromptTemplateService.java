package com.athena.ai.llm;

import java.util.Map;

public interface PromptTemplateService {

    String render(String templateName, Map<String, String> variables);
}
