package com.athena.llm;

import com.athena.llm.model.ChatRequest;
import com.athena.llm.model.ChatResult;

public interface ChatProvider {

    ChatResult complete(ChatRequest request);

    String model();

    String name();
}
