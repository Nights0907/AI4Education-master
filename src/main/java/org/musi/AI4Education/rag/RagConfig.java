package org.musi.AI4Education.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
// 移除了EmbeddingStoreIngestor import，现在由DocumentVectorService处理
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
// 移除了Comparator import，未使用
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class RagConfig {

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;


    @Bean
    public ContentRetriever contentRetriever() {
        // 注意：文档向量化现在由DocumentVectorService在启动时自动处理
        // 这里只需要配置检索器，不需要重新嵌入文档
        
        // 初始化Lucene索引（用于BM25检索）
        initializeLuceneIndex();

        return query -> retrieveWithBm25AndRerank(String.valueOf(query));
    }
    
    /**
     * 初始化Lucene索引
     */
    private void initializeLuceneIndex() {
        try {
            // 加载文档用于构建Lucene索引
            List<Document> documents = ClassPathDocumentLoader.loadDocuments("documents");
            DocumentByParagraphSplitter paragraphSplitter = new DocumentByParagraphSplitter(300, 100);
            
            org.apache.lucene.index.IndexWriter writer = new org.apache.lucene.index.IndexWriter(
                    luceneDirectory,
                    new org.apache.lucene.index.IndexWriterConfig(luceneAnalyzer)
            );
            
            for (Document doc : documents) {
                String fileName = doc.metadata().getString("file_name");
                java.util.List<dev.langchain4j.data.segment.TextSegment> segments = paragraphSplitter.split(doc);
                for (dev.langchain4j.data.segment.TextSegment seg : segments) {
                    String segText = seg.text();
                    org.apache.lucene.document.Document ldoc = new org.apache.lucene.document.Document();
                    ldoc.add(new org.apache.lucene.document.StringField("file_name", fileName == null ? "" : fileName, org.apache.lucene.document.Field.Store.YES));
                    ldoc.add(new org.apache.lucene.document.TextField("content", segText == null ? "" : segText, org.apache.lucene.document.Field.Store.YES));
                    writer.addDocument(ldoc);
                }
            }
            writer.commit();
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Lucene index", e);
        }
    }

    private org.apache.lucene.store.Directory luceneDirectory;
    private org.apache.lucene.analysis.Analyzer luceneAnalyzer;

    @PostConstruct
    public void initLucene() {
        try {
            this.luceneDirectory = new org.apache.lucene.store.MMapDirectory(java.nio.file.Files.createTempDirectory("rag-index"));
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to initialize Lucene directory", e);
        }
        this.luceneAnalyzer = new org.apache.lucene.analysis.standard.StandardAnalyzer();
    }

    private List<Content> retrieveWithBm25AndRerank(String query) {
        // 语义召回
        ContentRetriever semanticRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(qwenEmbeddingModel)
                .maxResults(5)
                .minScore(0.8)
                .build();
        List<Content> semantic = semanticRetriever.retrieve(Query.from(query));

        // BM25 召回
        List<Content> bm25 = bm25Retrieve(query, 12);

        // 合并去重
        List<Content> merged = mergeUniqueByText(semantic, bm25);

        // 可选：Cohere Rerank (REST) —— 当前实现为容错占位，返回原序
        List<Content> finalRanked = rerankViaCohereRest(query, merged);

        System.out.println("RAG query: " + query);
        System.out.println("RAG semantic: " + semantic.size() + ", bm25: " + bm25.size() + ", merged: " + merged.size() + ", final: " + finalRanked.size());
//        for (Content c : finalRanked) {
//            System.out.println("RAG segment: " + c);
//        }
        return finalRanked;
    }

    private static final int MAX_SNIPPET_LEN = 500;
    private static final int MAX_PER_FILE = 2;

    private List<Content> bm25Retrieve(String queryText, int k) {
        try {
            org.apache.lucene.search.IndexSearcher searcher = new org.apache.lucene.search.IndexSearcher(org.apache.lucene.index.DirectoryReader.open(luceneDirectory));
            searcher.setSimilarity(new org.apache.lucene.search.similarities.BM25Similarity());
            org.apache.lucene.queryparser.classic.QueryParser parser = new org.apache.lucene.queryparser.classic.QueryParser("content", luceneAnalyzer);
            org.apache.lucene.search.Query q = parser.parse(org.apache.lucene.queryparser.classic.QueryParser.escape(queryText));
            org.apache.lucene.search.TopDocs topDocs = searcher.search(q, k);
            List<Content> res = new ArrayList<>();
            java.util.Map<String, Integer> countPerFile = new java.util.HashMap<>();
            for (org.apache.lucene.search.ScoreDoc sd : topDocs.scoreDocs) {
                org.apache.lucene.document.Document d = searcher.doc(sd.doc);
                String fileName = d.get("file_name");
                String content = d.get("content");
                // limit number of snippets per file
                int used = countPerFile.getOrDefault(fileName, 0);
                if (used >= MAX_PER_FILE) continue;
                countPerFile.put(fileName, used + 1);
                // truncate long snippets
                if (content != null && content.length() > MAX_SNIPPET_LEN) {
                    content = content.substring(0, MAX_SNIPPET_LEN) + "...";
                }
                dev.langchain4j.data.document.Metadata meta = new dev.langchain4j.data.document.Metadata();
                meta.put("file_name", fileName);
                TextSegment seg = TextSegment.from(content, meta);
                res.add(Content.from(seg));
            }
            return res;
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Content> mergeUniqueByText(List<Content> a, List<Content> b) {
        List<Content> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Content c : a) {
            String key = String.valueOf(c);
            if (seen.add(key)) merged.add(c);
        }
        for (Content c : b) {
            String key = String.valueOf(c);
            if (seen.add(key)) merged.add(c);
        }
        return merged;
    }

    @Value("${cohere.api-key:}")
    private String cohereApiKey;

    private List<Content> rerankViaCohereRest(String query, List<Content> contents) {
        if (cohereApiKey == null || cohereApiKey.isBlank() || contents.isEmpty()) return contents;
        try {
            java.util.List<String> docs = contents.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());

            java.util.HashMap<String, Object> payload = new java.util.HashMap<>();
            payload.put("model", "rerank-english-v3.0");
            payload.put("query", query);
            payload.put("documents", docs);
            payload.put("top_n", Math.min(docs.size(), 10));

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.cohere.ai/v1/rerank"))
                    .header("Authorization", "Bearer " + cohereApiKey)
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(new com.alibaba.fastjson.JSONObject(payload).toJSONString()))
                    .build();
            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return contents;

            com.alibaba.fastjson.JSONObject root = com.alibaba.fastjson.JSONObject.parseObject(resp.body());
            com.alibaba.fastjson.JSONArray results = root.getJSONArray("results");
            if (results == null || results.isEmpty()) return contents;

            java.util.List<Integer> order = new java.util.ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                com.alibaba.fastjson.JSONObject item = results.getJSONObject(i);
                Integer idx = item.getInteger("index");
                if (idx != null) order.add(idx);
            }
            java.util.List<Content> reranked = new java.util.ArrayList<>();
            for (Integer idx : order) {
                if (idx >= 0 && idx < contents.size()) reranked.add(contents.get(idx));
            }
            return reranked.isEmpty() ? contents : reranked;
        } catch (Exception e) {
            return contents;
        }
    }
}