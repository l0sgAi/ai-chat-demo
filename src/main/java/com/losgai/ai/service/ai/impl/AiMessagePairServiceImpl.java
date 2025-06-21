package com.losgai.ai.service.ai.impl;

import com.losgai.ai.entity.ai.AiMessagePair;
import com.losgai.ai.entity.ai.AiSession;
import com.losgai.ai.mapper.AiMessagePairMapper;
import com.losgai.ai.mapper.AiSessionMapper;
import com.losgai.ai.service.ai.AiMessagePairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMessagePairServiceImpl implements AiMessagePairService {

    private final AiMessagePairMapper aiMessagePairMapper;

    private final AiSessionMapper aiSessionMapper;

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

}
