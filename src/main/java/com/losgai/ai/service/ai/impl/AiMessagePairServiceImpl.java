package com.losgai.ai.service.ai.impl;

import cn.dev33.satoken.stp.StpUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.losgai.ai.entity.ai.AiMessagePair;
import com.losgai.ai.entity.ai.AiSession;
import com.losgai.ai.mapper.AiMessagePairMapper;
import com.losgai.ai.mapper.AiSessionMapper;
import com.losgai.ai.service.ai.AiMessagePairService;
import com.losgai.ai.util.ElasticsearchIndexUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMessagePairServiceImpl implements AiMessagePairService {

    private final AiMessagePairMapper aiMessagePairMapper;

    private final AiSessionMapper aiSessionMapper;

    private final ElasticsearchClient esClient;

    @Override
    public List<AiMessagePair> selectBySessionId(Long sessionId) {
        return aiMessagePairMapper.selectBySessionId(sessionId);
    }

    @Override
    @Transactional
    public void addMessage(AiMessagePair aiMessage) {
        Date messageDate = Date.from(Instant.now());
        aiMessage.setCreateTime(messageDate);
        AiSession aiSession = new AiSession();
        aiSession.setLastMessageTime(messageDate);
        aiSession.setId(aiMessage.getSessionId());
        // 插入消息的同时更新会话的最后消息时间
        aiSessionMapper.updateByPrimaryKeySelective(aiSession);
        aiMessagePairMapper.insert(aiMessage);
    }

    @Override
    public void deleteBySessionId(Long id) {
        aiMessagePairMapper.deleteBySessionId(id);
    }

    @Override
    @Description("文档批量插入Elasticsearch，返回状态true/false")
    public boolean insertAiMessagePairDocBatch(String indexName,List<AiMessagePair> aiMessagePairs) throws IOException {
        // 1.判断是否有索引，没有则创建
        ElasticsearchIndexUtils.createIndex(indexName, esClient);

        // 2.插入一系列文档
        // 将aiMessagePairs封装成BulkOperation对象
        List<BulkOperation> bulkOperations = aiMessagePairs.stream()
                .map(aiMessagePair ->
                        BulkOperation.of(builder ->
                                builder.index(i->i.index(indexName)
                                        .document(aiMessagePair)
                                        .id(String.valueOf(aiMessagePair.getId())))))
                .toList();

        // 使用bulk方法执行批量操作并获得响应
        BulkResponse response = esClient.bulk(e->e.index(indexName).operations(bulkOperations));
        log.info("ES批量插入操作完成，耗时{}毫秒", response.took());
        if (response.errors()) {
            log.error("ES批量操作-有错误发生");
            return false;
        }
        return true;
    }

    @Override
    @Description("文档单条插入Elasticsearch，返回状态true/false")
    public boolean insertAiMessagePairDoc(String indexName,AiMessagePair aiMessagePair) throws IOException {
        // 1.判断是否有索引，没有则创建
        ElasticsearchIndexUtils.createIndex(indexName, esClient);

        // 2.插入单条文档
        IndexRequest<AiMessagePair> indexRequest = IndexRequest.of(builder -> builder
                .index(indexName)
                .document(aiMessagePair)
                .id(String.valueOf(aiMessagePair.getId())) // 确保 AiMessagePair 有 getId() 方法
        );

        IndexResponse response = esClient.index(indexRequest);
        log.info("插入文档成功，ID: {}, 状态: {}", response.id(), response.result().jsonValue());
        return true;
    }

    @Override
    public List<AiMessagePair> getFromGlobalSearch(String indexName, String keyWord) throws IOException {
        // 1. 获取当前用户的会话对应的id列表
        Set<Long> sessionIds  = aiSessionMapper.selectAllIdsByUserId(StpUtil.getLoginIdAsLong());
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        // 2. 创建查询条件
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indexName)
                .size(50)
                .query(q -> q
                        .bool(b -> b
                                // 使用 multi_match 查询来匹配 userContent 或 aiContent 字段
                                .must(m -> m
                                        .multiMatch(mm -> mm
                                                .query(keyWord)
                                                .fields("userContent", "aiContent")
                                        )
                                )
                                // filter 子句保持不变，用于过滤会话ID
                                .filter(f -> f
                                        .terms(t -> t
                                                .field("sessionId")
                                                .terms(ts -> ts
                                                        .value(sessionIds.stream()
                                                                .map(FieldValue::of)
                                                                .toList()
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        // 3. 执行查询并解析结果
        SearchResponse<AiMessagePair> response = esClient.search(searchRequest, AiMessagePair.class);

        return response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}
