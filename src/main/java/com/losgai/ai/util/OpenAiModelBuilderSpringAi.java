package com.losgai.ai.util;

import com.losgai.ai.entity.AiConfig;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Flux;

public class OpenAiModelBuilderSpringAi {

    /**
     * @param aiConfig     传入的AI配置
     * @param systemMsg    系统提示词
     * @param userMsg      用户输入的问题
     * @param assistantMsg 给对话提供的背景信息
     * @return Flux<ChatResponse> 反应式对话流
     * @apiNote 创建一个OpenAi模型，流式返回结果
     */
    public static Flux<ChatResponse> buildModel(AiConfig aiConfig,
                                                String systemMsg,
                                                String userMsg,
                                                String assistantMsg) {

        // 为对话提供高级指令。例如，您可以使用系统消息指示生成器像某个角色一样行事，或以特定格式提供答案。
        Message sysMessage = new SystemMessage(systemMsg);
        // 用户输入的问题文本，它们代表问题、提示或您希望生成器响应的任何输入。
        Message userMessage = new UserMessage(userMsg);
        // 提供有关对话中先前交流的背景信息
        Message assistantMessage = new AssistantMessage(assistantMsg);

        String apiDomain = aiConfig.getApiDomain().replace("/v1", "");
        OpenAiApi openAiApi = OpenAiApi.builder()
                // 填入自己的API KEY
                .apiKey(aiConfig.getApiKey())
                // 填入自己的API域名，如果是百炼，即为https://dashscope.aliyuncs.com/compatible-mode
                // 注意：这里与langchain4j的配置不同，不需要在后面加/v1
                .baseUrl(apiDomain)
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

        // 提示词
        Prompt prompt = Prompt.builder()
                .chatOptions(chatOptions)
                // 输入的问题，messages和content设置只能有其中一个，建议在输入问题时使用messages
                // .content("你是谁？能做什么？之前我们在干什么？")
                // 添加系统、用户、背景信息
                .messages(sysMessage, userMessage, assistantMessage)
                .build();

        // 反应式对话流
        return model.stream(prompt);
    }
}
