package com.losgai.ai.util;

import cn.hutool.core.collection.CollUtil;
import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.memory.MybatisChatMemory;
import com.losgai.ai.service.ai.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatClientFactory {

    // 根据配置id来分配不同的Client，复用
    private final Map<Integer, ChatClient> clientCache = new ConcurrentHashMap<>();

    private final MybatisChatMemory mybatisChatMemory;

    private final RagService ragService;

    private final EmbeddingStoreFactory embeddingStoreFactory;

    private static final String INDEX_FINDING_SYSTEM_MSG =
            "You are an expert-level AI Routing Agent. " +
                    "Your sole task is to select the most relevant index name from a given list based on a user's question.\n" +
                    "\n" +
                    "Your workflow is as follows:\n" +
                    "1.  Analyze the user's question to understand its core intent and subject matter.\n" +
                    "2.  Examine each name in the \"Index Name List\" to understand the data domain it represents.\n" +
                    "3.  Perform a semantic match to determine which index is most likely to contain the information needed to answer the user's question.\n" +
                    "\n" +
                    "You must strictly adhere to the following rules:\n" +
                    "-   **Unique Output**: Your response MUST be one of the index names from the list, or the string \"0\".\n" +
                    "-   **No Explanation**: Do NOT include any explanations, justifications, apologies, or any form of additional text. For example, do not say \"I think the best match is a\". You must only output \"a\".\n" +
                    "-   **Definition of \"0\"**: If the user's question is small talk, a greeting, completely unrelated to any of the indexes, or if you cannot determine a clear correlation, you MUST output \"0\".\n" +
                    "-   **Exact Match**: The outputted index name must be an exact, case-sensitive match to the string provided in the list.\n" +
                    "-   **Decisive Choice**: When multiple options seem partially relevant, choose the one that is most centrally and directly related. If you cannot make a clear best choice, default to outputting \"0\" to avoid incorrect routing.";

    /**
     * 根据 AI 配置创建或复用 ChatClient
     */
    public ChatClient getOrCreateClient(AiConfig aiConfig) {
        return clientCache.computeIfAbsent(aiConfig.getId(), k -> createClient(aiConfig));
    }

    /**
     * 真正创建 ChatClient 的方法
     */
    private ChatClient createClient(AiConfig aiConfig) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(aiConfig.getApiKey())
                .baseUrl(aiConfig.getApiDomain())
                .completionsPath("/chat/completions")
                .build();

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .maxTokens(aiConfig.getMaxContextMsgs())
                .temperature(aiConfig.getTemperature())
                .topP(aiConfig.getSimilarityTopP())
                .model(aiConfig.getModelId())
                .streamUsage(true)
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatOptions)
                .build();

        return ChatClient.builder(chatModel)
                .build();
    }

    /**
     * 构建流式对话响应
     */
    public Flux<ChatResponse> streamChat(
            AiConfig aiConfig,
            List<String> urlList,
            String systemMsg,
            String userMsg,
            String conversationId) {

        ChatClient chatClient = getOrCreateClient(aiConfig);

        List<String> indexes = List.of();
        try {
            indexes = ragService.getIndexes();
        } catch (IOException e) {
            log.error("获取向量索引列表失败！");
        }

        // RAG检索
        Advisor retrievalAugmentationAdvisor = null;

        if (CollUtil.isNotEmpty(indexes)) {
            Set<String> indexSet = Set.copyOf(indexes);
            String INDEX_FINDING_USER_MSG = "Index Name List:" +
                    indexes +
                    "User Question:" +
                    userMsg;

            // 首先通过大模型，选择合适的RAG索引
            String indexName = chatClient.prompt()
                    .system(INDEX_FINDING_SYSTEM_MSG)
                    .user(INDEX_FINDING_USER_MSG)
                    .call()
                    .content();
            // 索引合法，构建检索器
            if (indexSet.contains(indexName)) {
                // 构建检索器
                ElasticsearchVectorStore vectorStore =
                        embeddingStoreFactory.createVectorStore(indexName);
                // 构建召回器
                retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(VectorStoreDocumentRetriever.builder()
                                // 相似度阈值，0.5表示只有当检索结果的相似度分数 ≥ 0.50 时，才返回上层
                                .similarityThreshold(0.10)
                                // 注入的向量存储
                                .vectorStore(vectorStore)
                                .build())
                        .queryAugmenter(ContextualQueryAugmenter.builder()
                                // 使用参数，允许查找的结果为空
                                .allowEmptyContext(true)
                                .build())
                        .build();
            }
        }else {
            log.info("跳过RAG步骤");
        }

        // 多模态输入
        if (CollUtil.isNotEmpty(urlList) && aiConfig.getModelType() == 2) {
            List<Media> mediaList = urlList.stream().map(url -> {
                try {
                    MimeType mimeType = url.endsWith("png") ? MimeTypeUtils.IMAGE_PNG : MimeTypeUtils.IMAGE_JPEG;
                    return Media.builder().mimeType(mimeType).data(new UrlResource(url)).build();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            if(retrievalAugmentationAdvisor == null){
                return chatClient.prompt()
                        .user(u -> u.text(userMsg).media(mediaList.toArray(new Media[0])))
                        .advisors(MessageChatMemoryAdvisor.builder(mybatisChatMemory).build())
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .stream()
                        .chatResponse();
            }
            return chatClient.prompt()
                    .user(u -> u.text(userMsg).media(mediaList.toArray(new Media[0])))
                    .advisors(MessageChatMemoryAdvisor.builder(mybatisChatMemory).build())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .advisors(retrievalAugmentationAdvisor)
                    .stream()
                    .chatResponse();
        }

        if(retrievalAugmentationAdvisor == null){
            return chatClient.prompt()
                    .user(userMsg)
                    .advisors(MessageChatMemoryAdvisor.builder(mybatisChatMemory).build())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .chatResponse();
        }

        // 普通文本
        return chatClient.prompt()
                .user(userMsg)
                .advisors(MessageChatMemoryAdvisor.builder(mybatisChatMemory).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(retrievalAugmentationAdvisor)
                .stream()
                .chatResponse();
    }
}