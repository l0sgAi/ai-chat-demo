package com.losgai.ai.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.losgai.ai.config.PromptProperties;
import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.global.SseEmitterManager;
import com.losgai.ai.memory.MybatisChatMemory;
import com.losgai.ai.service.ai.RagService;
import com.losgai.ai.tools.DateTimeTools;
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
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatClientFactory {

    private final Map<Integer, ChatClient> clientCache = new ConcurrentHashMap<>();

    private final MybatisChatMemory mybatisChatMemory;

    private final RagService ragService;

    private final EmbeddingStoreFactory embeddingStoreFactory;

    private final SseEmitterManager sseEmitterManager;

    private final CustomMcpToolCallbackProvider toolCallbackProvider;

    private final PromptProperties promptProperties;

    @Value("${ai-chat-demo.openai.pool-max-connections:200}")
    private int poolMaxConnections;

    @Value("${ai-chat-demo.openai.pool-acquire-timeout-seconds:30}")
    private int poolAcquireTimeoutSeconds;

    @Value("${ai-chat-demo.openai.response-timeout-seconds:120}")
    private int responseTimeoutSeconds;

    @Value("${ai-chat-demo.openai.index-finding-timeout-seconds:30}")
    private int indexFindingTimeoutSeconds;

    public ChatClient getOrCreateClient(AiConfig aiConfig) {
        return clientCache.computeIfAbsent(aiConfig.getId(), k -> createClient(aiConfig));
    }

    public void evictClient(Integer configId) {
        ChatClient removed = clientCache.remove(configId);
        if (removed != null) {
            log.info("已淘汰配置ID={}的ChatClient缓存", configId);
        }
    }

    public void evictAllClients() {
        clientCache.clear();
        log.info("已清空全部ChatClient缓存");
    }

    private ChatClient createClient(AiConfig aiConfig) {
        ConnectionProvider provider = ConnectionProvider.builder("openai-" + aiConfig.getId())
                .maxConnections(poolMaxConnections)
                .pendingAcquireTimeout(Duration.ofSeconds(poolAcquireTimeoutSeconds))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .responseTimeout(Duration.ofSeconds(responseTimeoutSeconds));

        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(aiConfig.getApiKey())
                .baseUrl(aiConfig.getApiDomain())
                .completionsPath("/chat/completions")
                .webClientBuilder(webClientBuilder)
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
                .defaultTools(new DateTimeTools())
                .defaultSystem(promptProperties.getDefaultSystemMsg())
                .build();
    }

    /**
     * 安全发送SSE事件，防止emitter已关闭时抛出未捕获异常
     */
    private boolean safeSend(SseEmitter emitter, String sessionId, SseEmitter.SseEventBuilder event) {
        try {
            sseEmitterManager.touchActivity(sessionId);
            emitter.send(event);
            return true;
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE状态发送失败, emitter可能已关闭, sessionId: {}, 错误: {}", sessionId, e.getMessage());
            return false;
        }
    }

    public Flux<ChatResponse> streamChat(
            AiConfig aiConfig,
            List<String> urlList,
            String userMsg,
            String conversationId,
            String sessionId,
            SseEmitter emitter) {

        if (!safeSend(emitter, sessionId, SseEmitter.event().name("status").data("正在连接服务器..."))) {
            return Flux.error(new IllegalStateException("SSE连接已断开"));
        }

        ChatClient chatClient = getOrCreateClient(aiConfig);

        List<String> indexes = List.of();
        try {
            indexes = ragService.getIndexes();
        } catch (IOException e) {
            log.error("获取向量索引列表失败！");
        }

        Advisor retrievalAugmentationAdvisor = null;

        if (CollUtil.isNotEmpty(indexes)) {

            if (!safeSend(emitter, sessionId, SseEmitter.event().name("status").data("正在检索知识库..."))) {
                return Flux.error(new IllegalStateException("SSE连接已断开"));
            }

            Set<String> indexSet = Set.copyOf(indexes);
            String INDEX_FINDING_USER_MSG = "Index Name List:" +
                    indexes +
                    "User Question:" +
                    userMsg;

            String indexName = null;
            try {
                indexName = CompletableFuture.supplyAsync(() ->
                        chatClient.prompt()
                                .system(promptProperties.getIndexFindingSystemMsg())
                                .user(INDEX_FINDING_USER_MSG)
                                .call()
                                .content()
                ).get(indexFindingTimeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.warn("RAG索引查找超时({}s)，跳过RAG步骤, sessionId: {}", indexFindingTimeoutSeconds, sessionId);
            } catch (ExecutionException e) {
                log.error("RAG索引查找失败, sessionId: {}", sessionId, e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("RAG索引查找被中断, sessionId: {}", sessionId);
            }

            if (StrUtil.isNotBlank(indexName) && indexSet.contains(indexName)) {
                ElasticsearchVectorStore vectorStore = embeddingStoreFactory
                        .createVectorStore(indexName);
                retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(VectorStoreDocumentRetriever.builder()
                                .similarityThreshold(0.1)
                                .vectorStore(vectorStore)
                                .build())
                        .queryAugmenter(ContextualQueryAugmenter.builder()
                                .allowEmptyContext(true)
                                .build())
                        .build();
            }
        } else {
            log.info("跳过RAG步骤");
        }

        if (!safeSend(emitter, sessionId, SseEmitter.event().name("status").data("正在检索..."))) {
            return Flux.error(new IllegalStateException("SSE连接已断开"));
        }

        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();

        if (CollUtil.isNotEmpty(urlList) && aiConfig.getModelType() == 2) {
            List<Media> mediaList = urlList.stream().map(url -> {
                try {
                    MimeType mimeType = url.endsWith("png") ? MimeTypeUtils.IMAGE_PNG
                            : MimeTypeUtils.IMAGE_JPEG;
                    return Media.builder().mimeType(mimeType).data(new UrlResource(url)).build();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            if (retrievalAugmentationAdvisor == null) {
                return chatClient.prompt()
                        .toolCallbacks(toolCallbacks)
                        .user(u -> u.text(userMsg).media(mediaList.toArray(new Media[0])))
                        .advisors(MessageChatMemoryAdvisor.builder(mybatisChatMemory).build())
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .stream()
                        .chatResponse();
            }
            return chatClient.prompt()
                    .toolCallbacks(toolCallbacks)
                    .user(u -> u.text(userMsg).media(mediaList.toArray(new Media[0])))
                    .advisors(MessageChatMemoryAdvisor.builder(mybatisChatMemory).build())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .advisors(retrievalAugmentationAdvisor)
                    .stream()
                    .chatResponse();
        }

        if (retrievalAugmentationAdvisor == null) {
            return chatClient.prompt()
                    .toolCallbacks(toolCallbacks)
                    .user(userMsg)
                    .advisors(MessageChatMemoryAdvisor.builder(mybatisChatMemory).build())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .chatResponse();
        }

        return chatClient.prompt()
                .toolCallbacks(toolCallbacks)
                .user(userMsg)
                .advisors(MessageChatMemoryAdvisor.builder(mybatisChatMemory).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(retrievalAugmentationAdvisor)
                .stream()
                .chatResponse();
    }
}
