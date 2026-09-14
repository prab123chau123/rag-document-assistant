package com.RAGPipeline.service;

import com.RAGPipeline.RagDtos.RagDto;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentService {

    private static final int MAX_TEXT_CHARACTERS = 200_000;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public DocumentService(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore) {

        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public RagDto.UploadResponse upload(MultipartFile file) {

        if (file.isEmpty()) {
            throw badRequest("The uploaded file is empty.");
        }

        String filename = safeFilename(file.getOriginalFilename());
        String text = extractText(file, filename);

        if (text.isBlank()) {
            throw badRequest(
                    "No text found. Scanned PDFs need OCR before uploading.");
        }

        if (text.length() > MAX_TEXT_CHARACTERS) {
            throw badRequest(
                    "Document is too large for this example. "
                            + "Use at most 200,000 extracted characters.");
        }

        UUID documentId = UUID.randomUUID();

        /*
         * This overload uses character counts, not token counts.
         * Target: 1,000 characters per chunk, with up to 150 overlap.
         */
        List<TextSegment> chunks = DocumentSplitters
                .recursive(1000, 150)
                .split(Document.from(text));

        for (int index = 0; index < chunks.size(); index++) {
            chunks.get(index).metadata()
                    .put("documentId", documentId.toString())
                    .put("filename", filename)
                    .put("chunkIndex", index);
        }

        /*
         * Nomic uses different task prefixes for stored content
         * and search queries.
         *
         * Keep prefixes out of the original text that we store.
         */
        List<Embedding> embeddings = new ArrayList<>();

        for (TextSegment chunk : chunks) {
            Embedding embedding = embeddingModel
                    .embed("search_document: " + chunk.text())
                    .content();

            embeddings.add(embedding);
        }

        // Generate all embeddings before attempting the database write.
        embeddingStore.addAll(embeddings, chunks);

        return new RagDto.UploadResponse(
                documentId,
                filename,
                chunks.size());
    }

    private String extractText(MultipartFile file, String filename) {
        String lowerCaseName = filename.toLowerCase(Locale.ROOT);

        try {
            byte[] bytes = file.getBytes();

            if (lowerCaseName.endsWith(".txt")) {
                return decodeUtf8(bytes);
            }

            if (lowerCaseName.endsWith(".pdf")) {
                try (PDDocument pdf = Loader.loadPDF(bytes)) {
                    return new PDFTextStripper().getText(pdf);
                }
            }

            throw badRequest("Only .txt and .pdf files are supported.");

        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot read this file. Use a valid UTF-8 TXT "
                            + "or an unencrypted text-based PDF.",
                    exception);
        }
    }

    private String decodeUtf8(byte[] bytes)
            throws CharacterCodingException {

        return StandardCharsets.UTF_8
                .newDecoder()
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw badRequest("A filename is required.");
        }

        String normalized = originalFilename.replace('\\', '/');
        String filename = normalized.substring(
                normalized.lastIndexOf('/') + 1);

        if (filename.isBlank() || filename.length() > 200) {
            throw badRequest("Filename must contain 1–200 characters.");
        }

        return filename.replaceAll("\\p{Cntrl}", "_");
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message);
    }
}