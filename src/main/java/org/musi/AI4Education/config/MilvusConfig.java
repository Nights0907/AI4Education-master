package org.musi.AI4Education.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Milvus向量数据库配置
 * 使用Milvus SDK 2.6.2和langchain4j-milvus 1.1.0
 */
@Configuration
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    @Value("${milvus.database:default}")
    private String database;

    @Value("${milvus.collection:default}")
    private String collection;

    @Value("${milvus.dimension:1536}")
    private int dimension;

    /**
     * 配置Milvus向量存储
     * 使用@Primary注解确保这个Bean优先于内存存储
     */
    @Bean
    @Primary
    public EmbeddingStore<TextSegment> milvusEmbeddingStore() {
        try {
            System.out.println("尝试初始化Milvus向量存储...");
            System.out.println("Milvus配置信息 - Host: " + host + ", Port: " + port + ", Collection: " + collection);
            
            MilvusEmbeddingStore.Builder builder = MilvusEmbeddingStore.builder()
                    .uri("http://" + host + ":" + port)
                    .collectionName(collection)
                    .dimension(dimension);
            
            System.out.println("Milvus连接配置：无认证模式");
            EmbeddingStore<TextSegment> store = builder.build();
            System.out.println("Milvus向量存储初始化成功！");
            return store;
            
        } catch (NoClassDefFoundError e) {
            System.err.println("Milvus SDK版本不兼容: " + e.getMessage());
            System.err.println("将使用内存存储作为备用方案");
            return new InMemoryEmbeddingStore<>();
        } catch (Exception e) {
            System.err.println("Milvus向量存储初始化失败: " + e.getMessage());
            System.err.println("将使用内存存储作为备用方案");
            return new InMemoryEmbeddingStore<>();
        }
    }

    /**
     * 提供原生 MilvusClient 以便进行集合统计等管理操作
     */
    @org.springframework.context.annotation.Lazy
    @Bean
    public io.milvus.client.MilvusClient milvusClient() {
        return new io.milvus.client.MilvusServiceClient(
                io.milvus.param.ConnectParam.newBuilder()
                        .withHost(host)
                        .withPort(port)
                        .build()
        );
    }
}
