package com.RAGPipeline.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class QuestionServiceTest {
    private EmbeddingMatch<TextSegment> chunk(int index, double score, String text) {
        TextSegment segment = TextSegment.from(text);
        segment.metadata().put("chunkIndex", index);
        return new EmbeddingMatch<>(score, "chunk-" + index, null, segment);
    }

    @Test
    void shortDocumentKeepsLowScoringCompanyHeadingsInDocumentOrder() {
        var candidates = List.of(chunk(2, 0.9, "Responsibilities"),
                chunk(0, 0.2, "Company A, 2020-2022"), chunk(1, 0.3, "Company B, 2022-2024"));
        var selected = QuestionService.selectMatches(candidates, 1, 0.65);
        assertEquals(List.of("chunk-0", "chunk-1", "chunk-2"),
                selected.stream().map(EmbeddingMatch::embeddingId).toList());
    }

    @Test
    void largerDocumentUsesSimilarityLimitAndThreshold() {
        var candidates = IntStream.range(0, 17)
                .mapToObj(i -> chunk(i, i / 20.0, "text")).toList();
        var selected = QuestionService.selectMatches(candidates, 2, 0.65);
        assertEquals(List.of("chunk-15", "chunk-16"),
                selected.stream().map(EmbeddingMatch::embeddingId).toList());
        assertTrue(QuestionService.selectMatches(candidates, 4, 0.99).isEmpty());
    }

    @Test
    void characterBudgetAlsoLimitsFullDocumentContext() {
        var candidates = List.of(chunk(0, 0.2, "x".repeat(16000)), chunk(1, 0.9, "match"));
        assertEquals(List.of(candidates.get(1)), QuestionService.selectMatches(candidates, 4, 0.65));
    }
}
