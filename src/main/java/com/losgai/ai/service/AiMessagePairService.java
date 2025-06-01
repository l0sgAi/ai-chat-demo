package com.losgai.ai.service;


import com.losgai.ai.entity.AiMessagePair;

import java.util.List;

public interface AiMessagePairService {

    List<AiMessagePair> selectBySessionId(Long sessionId);

    void addMessage(AiMessagePair aiMessage);

    void deleteBySessionId(Long id);
}
