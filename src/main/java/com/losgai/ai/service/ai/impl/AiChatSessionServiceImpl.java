package com.losgai.ai.service.ai.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.losgai.ai.entity.ai.AiSession;
import com.losgai.ai.mapper.AiMessagePairMapper;
import com.losgai.ai.mapper.AiSessionMapper;
import com.losgai.ai.service.ai.AiChatSessionService;
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
public class AiChatSessionServiceImpl implements AiChatSessionService {

    private final AiSessionMapper aiSessionMapper;

    private final AiMessagePairMapper aiMessagePairMapper;

    /**
     * 新增单条会话
     */
    @Override
    public Long addSession(AiSession aiSession) {
        aiSession.setUserId(StpUtil.getLoginIdAsLong());
        aiSession.setCreatedTime(Date.from(Instant.now()));
        aiSession.setLastMessageTime(Date.from(Instant.now()));
        log.info("会话准备插入数据库:{}", aiSession);
        aiSessionMapper.insert(aiSession);
        // 返回插入的id
        return aiSession.getId();
    }

    /**
     * 根据keyword查询
     */
    @Override
    public List<AiSession> selectByKeyword(String keyword) {
        return aiSessionMapper.selectAllByUserId(keyword, StpUtil.getLoginIdAsLong());
    }

    /**
     * 级联删除
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        aiSessionMapper.deleteByPrimaryKey(id);
//        aiMessageMapper.deleteBySessionId(id);
        aiMessagePairMapper.deleteBySessionId(id);
    }

}
