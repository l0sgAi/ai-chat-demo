package com.losgai.ai.service.ai.impl;

import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.global.ChatSession;
import com.losgai.ai.service.ai.AiChatMessageService;
import com.losgai.ai.util.OpenAiModelBuilder;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Service;

@Service
public class AiChatMessageServiceImpl implements AiChatMessageService {

    public ChatSession chatMessageStream(AiConfig config, String userMessage) {
        OpenAiStreamingChatModel model = OpenAiModelBuilder.fromAiConfigByLangChain4j(config);
        return new ChatSession(model, userMessage);
    }
}
