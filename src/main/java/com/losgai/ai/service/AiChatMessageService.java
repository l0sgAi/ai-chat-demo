package com.losgai.ai.service;

import com.losgai.ai.entity.AiConfig;
import com.losgai.ai.global.ChatSession;

public interface AiChatMessageService {

    ChatSession chatMessageStream(AiConfig config, String userMessage);
}
