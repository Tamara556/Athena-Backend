package com.athena.llm;

import com.athena.llm.model.EmbeddingResult;

import java.util.List;

public interface EmbeddingProvider {

    EmbeddingResult embed(List<String> inputs);

    float[] embedOne(String input);

    int dimension();

    String model();

    String name();
}
