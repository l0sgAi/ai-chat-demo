package com.losgai.ai.service.ai;


import com.losgai.ai.entity.ai.AiMessagePair;

import java.util.List;

public interface AiMessagePairService {

    List<AiMessagePair> selectBySessionId(Long sessionId);

    void addMessage(AiMessagePair aiMessage);

    void deleteBySessionId(Long id);
}
