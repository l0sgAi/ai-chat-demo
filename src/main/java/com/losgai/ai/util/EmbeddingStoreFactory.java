package com.losgai.ai.util;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.losgai.ai.config.AiEmbeddingConfig;
import com.losgai.ai.entity.ai.AiConfig;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingStoreFactory {

    private final Map<String, OpenAiEmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();

    private final RestClient restClient; // Elasticsearch低级客户端

    private final ElasticsearchClient esClient; // Elasticsearch Java Client (8.x)

    private final AiEmbeddingConfig aiEmbeddingConfig;

    /**
     * 获取或创建 OpenAI Embedding 模型
     */
    public OpenAiEmbeddingModel getOrCreateEmbeddingModel(AiConfig aiConfig) {
        return embeddingModelCache.computeIfAbsent(
                aiConfig.getModelId(),
                k -> aiEmbeddingConfig.openAiEmbeddingModel());
    }

    /**
     * 创建一个 Elasticsearch Vector Store（允许自定义索引名）
     */
    public ElasticsearchVectorStore createVectorStore(String indexName) {
        try {
            // 1获取嵌入模型
            OpenAiEmbeddingModel embeddingModel = aiEmbeddingConfig.openAiEmbeddingModel();

            // 确保索引存在（可安全调用多次）
            ElasticsearchIndexUtils.createIndex(indexName, esClient);

            // 构建存储配置
            ElasticsearchVectorStoreOptions esOptions = new ElasticsearchVectorStoreOptions();
            esOptions.setIndexName(indexName);
            esOptions.setDimensions(1536);
            esOptions.setSimilarity(SimilarityFunction.cosine);

            // 构造 Vector Store
            return ElasticsearchVectorStore.builder(restClient, embeddingModel)
                    .options(esOptions)
                    .build();

        } catch (Exception e) {
            log.error("创建向量存储失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建向量存储失败", e);
        }
    }

}
