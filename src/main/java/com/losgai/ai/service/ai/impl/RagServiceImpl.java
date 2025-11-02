package com.losgai.ai.service.ai.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
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

import static com.losgai.ai.util.FileUtils.detectLanguage;
import static com.losgai.ai.util.FileUtils.generateSummary;

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
    @CacheEvict(value = "IndexNames", allEntries = true)
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
                .filter(StrUtil::isNotBlank)
                .filter(name -> !name.startsWith(".")) // ✅ 过滤系统索引
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "IndexNames", allEntries = true)
    public void deleteByDocId(Long id) throws IOException {
        RagStore ragStore = ragStoreMapper.selectByPrimaryKey(id);
        String indexName = ragStore.getIndexName();
        ragStoreMapper.deleteByPrimaryKey(id);
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
    @CacheEvict(value = "IndexNames", allEntries = true)
    public void deleteByDocIdBatch(List<Long> ids) {
        List<RagStore> list = ragStoreMapper.selectByIds(ids);
        if (list == null || list.isEmpty()) {
            return;
        }

        // 1. 按 indexName 分组 -> Map<String, List<Long>>
        Map<String, List<Long>> groupedIndexName = list.stream()
                .collect(Collectors.groupingBy(
                        RagStore::getIndexName,
                        Collectors.mapping(RagStore::getId, Collectors.toList())
                ));

        // 2. 先删除数据库记录
        ragStoreMapper.deleteByPrimaryKeys(ids);

        // 3. 再删除 Elasticsearch 中的文档
        groupedIndexName.forEach((indexName, docIds) -> {
            try {
                // 构造 BulkRequest
                BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

                for (Long docId : docIds) {
                    bulkBuilder.operations(op -> op
                            .delete(d -> d
                                    .index(indexName)
                                    .id(String.valueOf(docId))
                            )
                    );
                }

                // 执行批量删除
                BulkResponse bulkResponse = esClient.bulk(bulkBuilder.build());

                // 检查是否有失败项
                if (bulkResponse.errors()) {
                    log.error("ES 批量删除出现部分失败: {}", bulkResponse.items().stream()
                            .filter(i -> i.error() != null)
                            .map(item -> item.error().reason())
                            .collect(Collectors.joining(", ")));
                } else {
                    log.info("ES 批量删除成功，共 {} 条，索引：{}", docIds.size(), indexName);
                }

            } catch (Exception e) {
                log.error("ES 批量删除失败，索引={}，错误={}", indexName, e.getMessage(), e);
            }
        });
    }


    @Override
    @CacheEvict(value = "IndexNames", allEntries = true)
    public void embedding(List<Long> ids) {
        // 2. 发到消息队列中，遍历分组，对每个分组进行向量化
        aiMessageSender.sendEmbeddingMessage(
                RabbitMQAiMessageConfig.EXCHANGE_NAME,
                RabbitMQAiMessageConfig.VECTOR_ROUTING_KEY,
                ids);
    }

}
