package com.losgai.ai.mq.consumer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.losgai.ai.config.RabbitMQAiMessageConfig;
import com.losgai.ai.dto.RagStoreDto;
import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.entity.ai.AiMessagePair;
import com.losgai.ai.entity.ai.RagStore;
import com.losgai.ai.global.EsConstants;
import com.losgai.ai.mapper.AiConfigMapper;
import com.losgai.ai.mapper.RagStoreMapper;
import com.losgai.ai.service.ai.AiMessagePairService;
import com.losgai.ai.util.ElasticsearchIndexUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiMessageConsumer {

    private final AiMessagePairService aiMessagePairEsService;

    private final AiConfigMapper aiConfigMapper;

    private final RagStoreMapper ragStoreMapper;

    private final ElasticsearchClient esClient;

    private final RestClient restClient;

    private static final Integer id = 2;

    @RabbitListener(queues = RabbitMQAiMessageConfig.QUEUE_NAME)
    public void receiveMessage(AiMessagePair message) {
        log.info("[MQ]消费者收到消息：{}", message);
        boolean result;
        try {
            result = aiMessagePairEsService.insertAiMessagePairDoc(EsConstants.INDEX_NAME_AI_MSG, message);
            if (result) {
                log.info("[MQ]成功插入 ES，消息ID: {}", message.getId());
            } else {
                log.warn("[MQ]插入 ES 失败，消息ID: {}", message.getId());
            }
        } catch (IOException e) {
            log.error("[MQ]插入 ES 异常，消息ID: {}", message.getId(), e);
        }

    }

    @RabbitListener(queues = RabbitMQAiMessageConfig.VECTOR_QUEUE_NAME)
    public void receiveMessageVector(List<Long> ids) {
        log.info("[MQ]向量批量嵌入消费者收到消息：{}", ids);
        // 1.查出id对应的列表
        List<RagStore> ragStores = ragStoreMapper.selectByIds(ids);
        // 2.封装成Document、按照RagStore的indexName进行分组
        Map<String, List<Document>> groupedDocuments = ragStores.stream()
                .collect(Collectors.groupingBy(
                        RagStore::getIndexName, // 按 indexName 分组
                        Collectors.mapping(     // 每个分组元素转换成 Document
                                ragStore -> Document.builder()
                                        .id(String.valueOf(ragStore.getId()))
                                        .text(ragStore.getContent())
                                        .metadata(Map.of(
                                                "title", ragStore.getTitle(),
                                                "doc_id", ragStore.getDocId()))
                                        .build(),
                                Collectors.toList()
                        )
                ));
        try {
            // 从数据库读取 id=2 的配置（文本嵌入模型）
            AiConfig config = aiConfigMapper.selectByPrimaryKey(id);
            if (config == null) {
                log.error("未找到ID={}的嵌入模型配置", id);
            }
            if (config != null && config.getIsEnabled() != null && config.getIsEnabled() == 1) {
                log.error("ID={}的嵌入模型嵌入模型已禁用", id);
            }

            // 根据 apiDomain 判定使用哪家服务
            String domain = null;
            String apiKey = null;
            String modelName = null;
            if (config != null) {
                domain = config.getApiDomain();
                apiKey = config.getApiKey();
                modelName = config.getModelId();
            }

            // 构造 EmbeddingOptions
            OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                    .model(modelName)
                    .dimensions(1536)
                    //.user("模型对用户的称呼")
                    .build();

            OpenAiApi openAiApi = OpenAiApi.builder()
                    // 填入自己的API KEY
                    .apiKey(apiKey)
                    // 填入自己的API域名，如果是百炼，即为https://dashscope.aliyuncs.com/compatible-mode
                    // 注意：这里与langchain4j的配置不同，不需要在后面加/v1
                    .baseUrl(domain)
                    .completionsPath("/chat/completions")
                    .embeddingsPath("/embeddings")
                    .build();

            OpenAiEmbeddingModel openAiEmbeddingModel = new OpenAiEmbeddingModel(
                    openAiApi,
                    MetadataMode.EMBED,
                    options);


            for (String indexName : groupedDocuments.keySet()) {
                // 尝试创建索引
                ElasticsearchIndexUtils.createIndex(indexName, esClient);
                ElasticsearchVectorStoreOptions esOptions = new ElasticsearchVectorStoreOptions();
                esOptions.setDimensions(1536);
                esOptions.setIndexName(indexName);
                esOptions.setSimilarity(SimilarityFunction.cosine);

                // 嵌入文档
                ElasticsearchVectorStore elasticsearchVectorStore = ElasticsearchVectorStore.builder(restClient, openAiEmbeddingModel)
                        .options(esOptions).build();
                elasticsearchVectorStore.add(groupedDocuments.get(indexName));
            }
            log.info("[MQ]成功插入向量至ES");
            // 更新状态为成功
            ragStoreMapper.updateStatusBatch(ids, 1, null);
        } catch (IOException e) {
            log.error("[MQ]插入 ES 异常", e);
            // 更新状态为失败
            ragStoreMapper.updateStatusBatch(ids, 2, e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQAiMessageConfig.VECTOR_SINGLE_QUEUE_NAME)
    public void receiveMessageVectorSingle(RagStoreDto message) {
        log.info("[MQ]向量嵌入消费者收到消息：{}", message);
        try {
            // 从数据库读取 id=2 的配置（文本嵌入模型）
            AiConfig config = aiConfigMapper.selectByPrimaryKey(id);
            if (config == null) {
                log.error("未找到ID={}的嵌入模型配置", id);
            }
            if (config != null && config.getIsEnabled() != null && config.getIsEnabled() == 1) {
                log.error("ID={}的嵌入模型嵌入模型已禁用", id);
            }

            // 根据 apiDomain 判定使用哪家服务
            String domain = null;
            String apiKey = null;
            String modelName = null;
            if (config != null) {
                domain = config.getApiDomain();
                apiKey = config.getApiKey();
                modelName = config.getModelId();
            }

            // 构造 EmbeddingOptions
            OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                    .model(modelName)
                    .dimensions(1536)
                    //.user("模型对用户的称呼")
                    .build();

            OpenAiApi openAiApi = OpenAiApi.builder()
                    // 填入自己的API KEY
                    .apiKey(apiKey)
                    // 填入自己的API域名，如果是百炼，即为https://dashscope.aliyuncs.com/compatible-mode
                    // 注意：这里与langchain4j的配置不同，不需要在后面加/v1
                    .baseUrl(domain)
                    .completionsPath("/chat/completions")
                    .embeddingsPath("/embeddings")
                    .build();

            OpenAiEmbeddingModel openAiEmbeddingModel = new OpenAiEmbeddingModel(
                    openAiApi,
                    MetadataMode.EMBED,
                    options);

            String indexName = message.getIndexName();

            // 尝试创建索引
            ElasticsearchIndexUtils.createIndex(indexName, esClient);
            ElasticsearchVectorStoreOptions esOptions = new ElasticsearchVectorStoreOptions();
            esOptions.setDimensions(1536);
            esOptions.setIndexName(indexName);
            esOptions.setSimilarity(SimilarityFunction.cosine);

            // 嵌入文档
            ElasticsearchVectorStore elasticsearchVectorStore = ElasticsearchVectorStore.builder(restClient, openAiEmbeddingModel)
                    .options(esOptions).build();
            List<Document> documents = List.of(
                    new Document(
                            String.valueOf(message.getId()),
                            message.getContent(),
                            Map.of("title", message.getTitle(),
                                    "doc_id", message.getDocId())));
            elasticsearchVectorStore.add(documents);
            // 更新状态为成功
            ragStoreMapper.updateStatus(message.getId(), 1);
            log.info("[MQ]成功插入向量至ES");
        } catch (IOException e) {
            log.error("[MQ]插入 ES 异常", e);
            // 更新状态为失败
            ragStoreMapper.updateStatus(message.getId(), 2);
        }

    }
}