package com.losgai.ai.service.ai.impl;

import cn.dev33.satoken.stp.StpUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import com.losgai.ai.common.sys.CursorPageInfo;
import com.losgai.ai.config.RabbitMQAiMessageConfig;
import com.losgai.ai.dto.RagStoreDto;
import com.losgai.ai.entity.ai.RagStore;
import com.losgai.ai.mapper.RagStoreMapper;
import com.losgai.ai.mq.sender.AiMessageSender;
import com.losgai.ai.service.ai.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.losgai.ai.util.FileUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagServiceImpl implements RagService {

    private final RagStoreMapper ragStoreMapper;

    private final ElasticsearchClient esClient;

    private final AiMessageSender aiMessageSender;

    @Override
    public CursorPageInfo<RagStore> selectDocByPage(String keyword,
                                                    String startTime,
                                                    String endTime,
                                                    Integer status,
                                                    String lastUpdateTime,
                                                    int pageSize) {
        List<RagStore> list = ragStoreMapper.query(
                keyword,
                startTime,
                endTime,
                status,
                lastUpdateTime,
                pageSize);
        Long total = ragStoreMapper.selectCount();
        return new CursorPageInfo<>(list, total);
    }

    @Override
    @CacheEvict("IndexNames")
    public void add(RagStoreDto rag) {

        if (rag.getDeleted() == null || rag.getDeleted() != 0) {
            // 说明没有经过上传文件流程
            rag.setDocId(UUID.randomUUID().toString());
            String content = rag.getContent();
            rag.setContentSummary(generateSummary(content));
            rag.setDocType("txt");
            rag.setFileSize(0L);
            rag.setLanguage(detectLanguage(content));
            rag.setStatus(0);
            rag.setDeleted(0);
            rag.setChunkIndex(0);
            rag.setChunkTotal(1);
        }
        rag.setCreatedTime(Date.from(Instant.now()));
        rag.setUpdatedTime(Date.from(Instant.now()));
        rag.setUserId(StpUtil.getLoginIdAsLong());
        ragStoreMapper.insert(rag);

        if (rag.getIsEmbedding()) {
            // 插入向量
            aiMessageSender.sendEmbeddingMessageSingle(
                    RabbitMQAiMessageConfig.EXCHANGE_NAME,
                    RabbitMQAiMessageConfig.VECTOR_SINGLE_ROUTING_KEY,
                    rag
            );
        }
    }

    @Override
    @Cacheable("IndexNames")
    public List<String> getIndexes() throws IOException {
        // 调用 cat.indices API 获取索引元数据
        IndicesResponse indices = esClient.cat().indices(builder -> builder);

        // 提取 index 字段（索引名称）
        return indices.valueBody().stream()
                .map(IndicesRecord::index)
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> !name.startsWith(".")) // ✅ 过滤系统索引
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict("IndexNames")
    public void deleteByDocId(Long id) throws IOException {
        ragStoreMapper.deleteByPrimaryKey(id);
        RagStore ragStore = ragStoreMapper.selectByPrimaryKey(id);
        String indexName = ragStore.getIndexName();
        if (ragStore.getStatus() == 1) {
            // 删除索引中的向量数据
            // 1. 构建删除请求
            DeleteRequest deleteRequest = DeleteRequest.of(d -> d
                    .index(indexName)
                    .id(String.valueOf(ragStore.getId()))
            );

            // 2. 执行删除操作
            DeleteResponse response = esClient.delete(deleteRequest);

            // 3. 根据响应结果判断并记录日志
            if (response.result() == Result.Deleted) {
                log.info("文档删除成功, Index: {}, ID: {}, 状态: {}", indexName, response.id(), response.result().jsonValue());
            } else if (response.result() == Result.NotFound) {
                // 如果文档不存在，也认为操作达到了目的（确保其不存在），返回true
                log.warn("尝试删除的文档不存在, Index: {}, ID: {}, 状态: {}", indexName, response.id(), response.result().jsonValue());
            } else {
                // 其他情况，如版本冲突等，视为失败
                log.error("文档删除失败, Index: {}, ID: {}, 状态: {}", indexName, response.id(), response.result().jsonValue());
            }
        }
    }

    @Override
    public void embedding(List<Long> ids) {
        // 1.查出id对应的列表
        List<RagStore> ragStores = ragStoreMapper.selectByIds(ids);
        // 2. 封装成Document、按照RagStore的indexName进行分组
        Map<String, List<Document>> groupedDocuments = ragStores.stream()
                .collect(Collectors.groupingBy(
                        RagStore::getIndexName, // 按 indexName 分组
                        Collectors.mapping(     // 每个分组元素转换成 Document
                                ragStore -> Document.builder()
                                        .text(ragStore.getContent())
                                        .metadata(Map.of(
                                                "title", ragStore.getTitle(),
                                                "doc_id", ragStore.getDocId()))
                                        .build(),
                                Collectors.toList()
                        )
                ));
        // 3. 发到消息队列中，遍历分组，对每个分组进行向量化
        aiMessageSender.sendEmbeddingMessage(
                RabbitMQAiMessageConfig.EXCHANGE_NAME,
                RabbitMQAiMessageConfig.VECTOR_ROUTING_KEY,
                groupedDocuments);
    }

}
