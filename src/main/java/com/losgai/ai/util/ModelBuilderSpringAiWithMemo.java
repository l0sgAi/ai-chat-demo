package com.losgai.ai.util;

import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.memory.MybatisChatMemory;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class ModelBuilderSpringAiWithMemo {

    // 注入自定义对话记忆实现
    private final MybatisChatMemory mybatisChatMemory;

    /**
     * @param aiConfig     传入的AI配置
     * @param systemMsg    系统提示词
     * @param userMsg      用户输入的问题
     * @return Flux<ChatResponse> 反应式对话流
     * @apiNote 创建一个OpenAi模型，流式返回结果
     */
    public Flux<ChatResponse> buildModelStreamWithMemo(AiConfig aiConfig,
                                                String systemMsg,
                                                String userMsg, String conversationId) {

        String apiDomain = aiConfig.getApiDomain();
        OpenAiApi openAiApi = OpenAiApi.builder()
                // 填入自己的API KEY
                .apiKey(aiConfig.getApiKey())
                // 填入自己的API域名，如果是百炼，即为https://dashscope.aliyuncs.com/compatible-mode
                // 注意：这里与langchain4j的配置不同，不需要在后面加/v1
                .baseUrl(apiDomain)
                .completionsPath("/chat/completions")
                .build();

        // 模型选项
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                // 模型生成的最大 tokens 数
                .maxTokens(aiConfig.getMaxContextMsgs())
                // 模型生成的 tokens 的概率质量范围，取值范围 0.0-1.0 越大的概率质量范围越大
                .topP(aiConfig.getSimilarityTopP())
                // 模型生成的 tokens 的随机度，取值范围 0.0-1.0 越大的随机度越大
                .temperature(aiConfig.getTemperature())
                // 模型名称
                .model(aiConfig.getModelId())
                // 打开流式对话token计数配置，默认为false
                .streamUsage(true)
                .build();

        // 工具调用管理器 暂时为空
        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

        // 重试机制，设置最多3次
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)
                .build();

        // 观测数据收集器
        ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

        ChatModel model = new OpenAiChatModel(openAiApi,
                chatOptions,
                toolCallingManager,
                retryTemplate,
                observationRegistry);

        // 创建一个ChatClient对象，用于调用模型进行带记忆对话
        ChatClient chatClient = ChatClient.builder(model)
                // TODO: 系统提示词和用户提示词混淆解决
                // 系统提示词
//                .defaultSystem(systemMsg)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(mybatisChatMemory).build())
                .build();

        // 返回反应式对话流
        return chatClient.prompt(userMsg)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .chatResponse();
    }
}
