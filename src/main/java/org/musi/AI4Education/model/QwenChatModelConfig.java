package org.musi.AI4Education.model;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
// 移除了EmbeddingStore相关的import，现在使用Milvus
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import javax.annotation.Resource;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Data
public class QwenChatModelConfig {

    @Value("${qwen.model:qwen-plus}")
    private String modelName;

    @Value("${qwen.streaming-model:qwen-max}")
    private String streamingModelName;

    @Value("${qwen.embedding-model:text-embedding-v1}")
    private String embeddingModelName;

    @Value("${qwen.api-key:}")
    private String apiKey;

    @Resource
    private ChatModelListener chatModelListener;

    @Bean
    public ChatModelListener chatModelListener() {
        return new ChatModelListener() {};
    }

    @Bean
    public ChatModel myQwenChatModel() {
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .listeners(List.of(chatModelListener))
                .build();
    }

    @Bean
    public StreamingChatModel streamingChatModel() {
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(streamingModelName)
                .build();
    }

    @Bean
    public EmbeddingModel qwenEmbeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .build();
    }

    // 注意：EmbeddingStore现在由MilvusConfig配置，使用Milvus进行持久化存储
    // 移除了内存存储的Bean定义
}