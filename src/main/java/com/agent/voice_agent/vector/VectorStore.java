package com.agent.voice_agent.vector;

import com.agent.voice_agent.dto.EmbeddedChunk;
import com.agent.voice_agent.dto.VectorSearchResult;

import java.util.List;

public interface VectorStore {

    void addAll(List<EmbeddedChunk> chunks);

    List<VectorSearchResult> search(
            List<Double> queryEmbedding,
            int topK
    );

    int size();

    void clear();
}