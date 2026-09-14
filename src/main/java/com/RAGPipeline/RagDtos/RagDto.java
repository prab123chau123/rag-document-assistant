package com.RAGPipeline.RagDtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class RagDto {

    private RagDto() {
    }

    public record UploadResponse(
            UUID documentId,
            String filename,
            int chunksStored) {
    }

    public record QuestionRequest(
            @NotBlank
            @Size(max = 2000)
            String question,

            @NotNull
            UUID documentId) {
    }

    public record Source(
            String reference,
            String filename,
            int chunkIndex,
            double score,
            String excerpt) {
    }

    public record AnswerResponse(
            String answer,
            List<Source> sources) {
    }
}