package com.RAGPipeline;

import com.RAGPipeline.RagDtos.RagDto.AnswerResponse;
import com.RAGPipeline.RagDtos.RagDto.QuestionRequest;
import com.RAGPipeline.RagDtos.RagDto.UploadResponse;
import com.RAGPipeline.service.DocumentService;
import com.RAGPipeline.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class RagController {

    private final DocumentService documentService;
    private final QuestionService questionService;

    public RagController(
            DocumentService documentService,
            QuestionService questionService) {

        this.documentService = documentService;
        this.questionService = questionService;
    }

    @PostMapping(
            value = "/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(
            @RequestPart("file") MultipartFile file) {

        return documentService.upload(file);
    }

    @PostMapping("/chat")
    public AnswerResponse ask(
            @Valid @RequestBody QuestionRequest request) {

        return questionService.ask(request);
    }
}