package org.musi.AI4Education.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 文档向量管理服务
 * 负责文档的向量化和持久化存储
 */
@Service
public class DocumentVectorService {

    @Autowired
    private EmbeddingModel qwenEmbeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    private boolean isInitialized = false;

    @Autowired
    private MilvusAdminService milvusAdminService;

    /**
     * 初始化文档向量存储
     * 检查是否已有数据，如果没有则进行初始化
     */
    @PostConstruct
    public void initializeDocumentVectors() {
        if (!isInitialized) {
            try {
                long existing = milvusAdminService.getRowCount();
                if (existing > 0) {
                    System.out.println("Milvus集合已存在数据，行数=" + existing + "，跳过初始化嵌入。");
                    isInitialized = true;
                    return;
                }
                System.out.println("开始初始化文档向量到Milvus...");
                ingestDocuments();
                System.out.println("文档向量初始化完成");
                isInitialized = true;
            } catch (Exception e) {
                System.err.println("文档向量初始化失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 将文档嵌入到向量存储中
     */
    private void ingestDocuments() {
        // 加载文档
        List<Document> documents = ClassPathDocumentLoader.loadDocuments("questions");
        System.out.println("加载文档数量: " + documents.size());

        // 使用段落分割器
        DocumentByParagraphSplitter paragraphSplitter = new DocumentByParagraphSplitter(300, 100);

        // 创建嵌入存储摄取器
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(paragraphSplitter)
                .textSegmentTransformer(textSegment -> TextSegment.from(
                        textSegment.metadata().getString("file_name") + "\n" + textSegment.text(),
                        textSegment.metadata()
                ))
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        // 执行嵌入和存储
        ingestor.ingest(documents);
    }

    /**
     * 重新初始化文档向量（用于更新文档时）
     */
    public void reinitializeDocumentVectors() {
        try {
            System.out.println("开始重新初始化文档向量...");
            ingestDocuments();
            System.out.println("文档向量重新初始化完成");
        } catch (Exception e) {
            System.err.println("文档向量重新初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 添加新文档到向量存储
     */
    public void addDocument(Document document) {
        try {
            DocumentByParagraphSplitter paragraphSplitter = new DocumentByParagraphSplitter(300, 100);
            
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(paragraphSplitter)
                    .textSegmentTransformer(textSegment -> TextSegment.from(
                            textSegment.metadata().getString("file_name") + "\n" + textSegment.text(),
                            textSegment.metadata()
                    ))
                    .embeddingModel(qwenEmbeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            ingestor.ingest(document);
            System.out.println("新文档已添加到向量存储: " + document.metadata().getString("file_name"));
        } catch (Exception e) {
            System.err.println("添加文档到向量存储失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
