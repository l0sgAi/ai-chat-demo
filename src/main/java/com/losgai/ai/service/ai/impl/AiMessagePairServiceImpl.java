package com.losgai.ai.service.ai.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.losgai.ai.entity.ai.AiMessagePair;
import com.losgai.ai.entity.ai.AiSession;
import com.losgai.ai.mapper.AiMessagePairMapper;
import com.losgai.ai.mapper.AiSessionMapper;
import com.losgai.ai.service.ai.AiMessagePairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

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
    public void insertAiMessagePairDoc(String indexName,List<AiMessagePair> aiMessagePairs) throws IOException {
        // 1.判断是否有索引
        ExistsRequest existsRequest = new ExistsRequest.Builder()
                .index(indexName)
                .build();

        boolean value = esClient.indices().exists(existsRequest).value();
        if (!value) {
            log.info("索引不存在，创建索引:{}", indexName);
            CreateIndexRequest createIndexRequest = CreateIndexRequest.of(builder ->
                    builder.index(indexName)
            );
            esClient.indices().create(createIndexRequest);
        }

        // 2.插入一系列文档
        //TODO :插入文档


    }

    @Override
    public List<AiMessagePair> getFromGlobalSearch(String indexName, String query) throws IOException {
        // TODO: 返回查询
        return List.of();
    }

}
