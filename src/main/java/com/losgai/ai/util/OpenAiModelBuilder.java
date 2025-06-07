package com.losgai.ai.util;

import com.losgai.ai.entity.AiConfig;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

public class OpenAiModelBuilder {

    /** 构建流式对话参数*/
    public static OpenAiStreamingChatModel fromAiConfigByLangChain4j(AiConfig config) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getApiDomain()) // 如 https://dashscope.aliyuncs.com/compatible-mode/v1
                .modelName(config.getModelId()) // 如 qwen-turbo-latest
                .temperature(config.getTemperature()) // 可从 config 拓展配置
                .topP(config.getSimilarityTopP())
                .maxTokens(config.getMaxContextMsgs())
                .apiKey(config.getApiKey())
                .build();
    }
}