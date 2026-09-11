package com.agent.voice_agent.vector;

import com.agent.voice_agent.dto.EmbeddedChunk;
import com.agent.voice_agent.dto.VectorSearchResult;
import com.github.jelmerk.knn.DistanceFunction;
import com.github.jelmerk.knn.DistanceFunctions;
import com.github.jelmerk.knn.Index;
import com.github.jelmerk.knn.SearchResult;
import com.github.jelmerk.knn.hnsw.HnswIndex;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HnswVectorStore implements VectorStore {

    private static final int MAX_ITEM_COUNT = 50000;

    private static final int M = 16;

    private static final int EF_CONSTRUCTION = 200;

    private static final int EF_SEARCH = 100;

    private final Map<String, EmbeddedChunk> chunks =
            new HashMap<>();

    private Index<
            String,
            float[],
            EmbeddedChunk,
            Float
            > index;

    private int dimension = -1;

    @Override
    public synchronized void addAll(
            List<EmbeddedChunk> embeddedChunks
    ) {

        if (embeddedChunks == null ||
                embeddedChunks.isEmpty()) {

            return;
        }

        for (EmbeddedChunk chunk : embeddedChunks) {
            validateChunk(chunk);
        }

        int embeddingDimension =
                embeddedChunks.get(0)
                        .embedding()
                        .size();

        initializeIndexIfNecessary(
                embeddingDimension
        );

        for (EmbeddedChunk chunk : embeddedChunks) {

            if (chunks.containsKey(chunk.id())) {
                continue;
            }

            if (chunk.embedding().size() != dimension) {
                throw new IllegalArgumentException(
                        "Embedding dimension mismatch for chunk: "
                                + chunk.id()
                );
            }

            /*
             * EmbeddedChunk itself implements Item<String, float[]>
             *
             * Therefore we add the chunk directly.
             */
            index.add(chunk);

            chunks.put(
                    chunk.id(),
                    chunk
            );
        }
    }

    @Override
    public synchronized List<VectorSearchResult> search(
            List<Double> queryEmbedding,
            int topK
    ) {

        if (queryEmbedding == null ||
                queryEmbedding.isEmpty()) {

            throw new IllegalArgumentException(
                    "Query embedding cannot be null or empty"
            );
        }

        if (topK <= 0) {
            throw new IllegalArgumentException(
                    "topK must be greater than zero"
            );
        }

        if (index == null ||
                chunks.isEmpty()) {

            return List.of();
        }

        if (queryEmbedding.size() != dimension) {

            throw new IllegalArgumentException(
                    "Query embedding dimension " +
                            queryEmbedding.size() +
                            " does not match indexed dimension " +
                            dimension
            );
        }

        float[] queryVector =
                toFloatArray(queryEmbedding);

        int resultCount =
                Math.min(
                        topK,
                        chunks.size()
                );

        List<SearchResult<
                EmbeddedChunk,
                Float
                >> results =
                index.findNearest(
                        queryVector,
                        resultCount
                );

        List<VectorSearchResult> searchResults =
                new ArrayList<>(results.size());

        for (SearchResult<
                EmbeddedChunk,
                Float
                > result : results) {

            EmbeddedChunk chunk =
                    result.item();

            double similarity =
                    convertDistanceToSimilarity(
                            result.distance()
                    );

            searchResults.add(
                    new VectorSearchResult(
                            chunk,
                            similarity
                    )
            );
        }

        return searchResults;
    }

    @Override
    public synchronized int size() {
        return chunks.size();
    }

    @Override
    public synchronized void clear() {

        chunks.clear();

        index = null;

        dimension = -1;
    }

    private void initializeIndexIfNecessary(
            int embeddingDimension
    ) {

        if (embeddingDimension <= 0) {

            throw new IllegalArgumentException(
                    "Embedding dimension must be greater than zero"
            );
        }

        if (index != null) {

            if (dimension != embeddingDimension) {

                throw new IllegalArgumentException(
                        "Embedding dimension does not match " +
                                "the existing vector index"
                );
            }

            return;
        }

        dimension = embeddingDimension;

        index =
                HnswIndex
                        .newBuilder(
                                dimension,
                                DistanceFunctions.FLOAT_COSINE_DISTANCE,
                                MAX_ITEM_COUNT
                        )
                        .withM(M)
                        .withEfConstruction(
                                EF_CONSTRUCTION
                        )
                        .withEf(
                                EF_SEARCH
                        )
                        .build();
    }

    private void validateChunk(
            EmbeddedChunk chunk
    ) {

        if (chunk == null) {

            throw new IllegalArgumentException(
                    "Embedded chunk cannot be null"
            );
        }

        if (chunk.id() == null ||
                chunk.id().isBlank()) {

            throw new IllegalArgumentException(
                    "Embedded chunk id cannot be null or blank"
            );
        }

        if (chunk.content() == null ||
                chunk.content().isBlank()) {

            throw new IllegalArgumentException(
                    "Embedded chunk content cannot be null or blank"
            );
        }

        if (chunk.embedding() == null ||
                chunk.embedding().isEmpty()) {

            throw new IllegalArgumentException(
                    "Embedded chunk embedding cannot be null or empty"
            );
        }
    }

    private float[] toFloatArray(
            List<Double> embedding
    ) {

        float[] vector =
                new float[embedding.size()];

        for (int i = 0;
             i < embedding.size();
             i++) {

            Double value =
                    embedding.get(i);

            if (value == null ||
                    !Double.isFinite(value)) {

                throw new IllegalArgumentException(
                        "Embedding contains an invalid value at index "
                                + i
                );
            }

            vector[i] =
                    value.floatValue();
        }

        return vector;
    }

    private double convertDistanceToSimilarity(
            float distance
    ) {

        /*
         * FLOAT_COSINE_DISTANCE returns a cosine distance.
         *
         * similarity = 1 - distance
         */
        return 1.0 - distance;
    }
}