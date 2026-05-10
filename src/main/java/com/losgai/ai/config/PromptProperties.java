package com.losgai.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai-chat-demo.prompts")
public class PromptProperties {

    /**
     * RAG 索引路由系统提示词
     */
    private String indexFindingSystemMsg;

    /**
     * 默认助手系统提示词
     */
    private String defaultSystemMsg;
}
