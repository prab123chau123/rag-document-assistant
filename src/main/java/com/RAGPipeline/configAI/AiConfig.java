package com.RAGPipeline.configAI;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Bean
    public ChatModel chatModel(
            @Value("${rag.ollama.base-url}")
            String baseUrl,
            @Value("${rag.ollama.chat-model}")
            String modelName){
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.0)
                .numCtx(8192)
                .timeout(Duration.ofMinutes(3))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${rag.ollama.base-url}")
            String baseUrl,
            @Value("${rag.ollama.embedding-model}")
            String modelName
    ){
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofMinutes(3))
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            EmbeddingModel embeddingModel,
            @Value("${rag.database.host}")
            String host,
            @Value("${rag.database.port}")
            int port,
            @Value("${rag.database.name}")
            String database,
            @Value("${rag.database.user}")
            String user,
            @Value("${rag.database.password}")
            String password,
            @Value("${rag.database.table}")
            String  table){
        int dimension = embeddingModel
                .embed("search_query: dimension check")
                .content()
                .vector()
                .length;
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(user)
                .password(password)
                .table(table)
                .dimension(dimension)
                .createTable(true)
                .dropTableFirst(false)
                .build();
    }
}
