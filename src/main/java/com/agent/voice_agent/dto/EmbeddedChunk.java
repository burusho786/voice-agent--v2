package com.agent.voice_agent.dto;

import com.github.jelmerk.knn.Item;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record EmbeddedChunk(
        String id,
        String content,
        List<Double> embedding,
        Map<String, String> metadata
) implements Item<String, float[]>, Serializable {

    @Override
    public String id() {
        return id;
    }

    @Override
    public float[] vector() {
        float[] vector = new float[embedding.size()];

        for (int i = 0; i < embedding.size(); i++) {
            Double value = embedding.get(i);

            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException(
                        "Embedding contains an invalid value at index " + i
                );
            }

            vector[i] = value.floatValue();
        }

        return vector;
    }

    @Override
    public int dimensions() {
        return embedding.size();
    }

    @Override
    public long version() {
        return 1L;
    }
}