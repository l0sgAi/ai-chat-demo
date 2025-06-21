package com.losgai.ai.service.ai;

import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.global.ChatSession;

public interface AiChatMessageService {

    ChatSession chatMessageStream(AiConfig config, String userMessage);
}
