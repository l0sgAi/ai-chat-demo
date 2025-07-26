package com.losgai.ai.util;

import cn.hutool.core.collection.CollUtil;
import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.memory.MybatisChatMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelBuilderSpringAiWithMemo {

    // 注入自定义对话记忆实现
    private final MybatisChatMemory mybatisChatMemory;

    /**
     * @param aiConfig  传入的AI配置
     * @param systemMsg 系统提示词
     * @param userMsg   用户输入的问题
     * @return Flux<ChatResponse> 反应式对话流
     * @apiNote 创建一个OpenAi模型，流式返回结果
     */
    public Flux<ChatResponse> buildModelStreamWithMemo(AiConfig aiConfig,
                                                       List<String> urlList,
                                                       String systemMsg,
                                                       String userMsg, String conversationId) {

        OpenAiApi openAiApi = OpenAiApi.builder()
                // 填入自己的API KEY
                .apiKey(aiConfig.getApiKey())
                // 填入自己的API域名，如果是百炼，即为https://dashscope.aliyuncs.com/compatible-mode
                // 注意：这里与langchain4j的配置不同，不需要在后面加/v1
                .baseUrl(aiConfig.getApiDomain())
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

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatOptions)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(mybatisChatMemory).build())
                .build();

        // 构建多模态资源列表并返回，2表示视觉模型
        if (CollUtil.isNotEmpty(urlList) && aiConfig.getModelType() == 2) {
            List<Media> mediaList = urlList.stream().map(url -> {
                try {
                    MimeType mimeType = url.endsWith("png") ? MimeTypeUtils.IMAGE_PNG : MimeTypeUtils.IMAGE_JPEG;
                    return Media.builder()
                            .mimeType(mimeType)
                            .data(new UrlResource(url))
                            .build();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            Media[] array = mediaList.toArray(new Media[0]);

            return chatClient.prompt()
                    .user(u -> {
                        // 多模态资源也一同被上传到模型，这里需要使用UrlResource
                        u.text(userMsg)
                                .media(array);
                    })
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .chatResponse();
        }

        // 返回反应式对话流
        return chatClient.prompt()
                // .system(systemMsg)
                .user(userMsg)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .chatResponse();

    }

}
