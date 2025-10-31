package com.losgai.ai;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.mapper.AiConfigMapper;
import com.losgai.ai.memory.MybatisChatMemory;
import com.losgai.ai.util.ElasticsearchIndexUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
public class AiApplicationTests {

    @Autowired
    private AiConfigMapper aiConfigMapper;

    @Autowired
    private RestClient restClient;

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private MybatisChatMemory mybatisChatMemory;

    private static final Integer id = 2;

    @Test
    void SpringAIRAGChat() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        AiConfig aiConfig = aiConfigMapper.selectByPrimaryKey(1);

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

        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        // 相似度阈值，0.5表示只有当检索结果的相似度分数 ≥ 0.50 时，才返回上层
                        .similarityThreshold(0.50)
                        // 注入的向量存储
                        .vectorStore(vectorStore)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        // 使用参数，允许查找的结果为空
                        .allowEmptyContext(true)
                        .build())
                .build();

        // 返回反应式对话流
        String userMsg = "我们刚才谈论了任何包含数据库的问题吗？怪物猎人荒野在2025年10月31日的在线玩家峰值为多少？";
        Flux<ChatResponse> chatResponseFlux = chatClient.prompt()
                .user(userMsg)
                // 这里还可以整合对话记忆
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "65"))
                // 这里可以指定Metadata元数据匹配筛选
                .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "title == '荒野'"))
                .advisors(retrievalAugmentationAdvisor)
                .stream()
                .chatResponse();

        chatResponseFlux
                .subscribe(
                        token -> {
                            // 获取当前输出内容片段
                            String text = "";
                            if (token.getResult() != null) {
                                text = token.getResult().getOutput().getText();
                                if (StrUtil.isNotBlank(text)) {
                                    log.info("当前段数据:{}", text);
                                }
                            }
                        },
                        // 反应式流在报错时会直接中断
                        e -> {
                            log.error("ai对话 流式输出报错:{}", e.getMessage());
                        }, // 错误处理
                        () -> {// 流结束
                            log.info("\n回答完毕！");
                        });

        // 等待响应完成，最多等待20秒
        boolean await = latch.await(20, TimeUnit.SECONDS);
        if (!await) {
            log.error("等待响应超时");
        }
    }

    /**
     * 通过数据库获取配置，初始化模型进行输出
     * 使用langchain4j
     */
    @Test
    void testLangChain4j() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        AiConfig aiConfig = aiConfigMapper.selectByPrimaryKey(1);

        // 构建模型对象，使用百炼 OpenAI 兼容模式
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                // 这里也可以直接填你的apiKey
                .apiKey(aiConfig.getApiKey())
                // 百炼域名地址 https://dashscope.aliyuncs.com/compatible-mode/v1
                .baseUrl(aiConfig.getApiDomain())
                // qwen-plus、qwen-max、qwen-turbo 等
                // qwen-turbo 比较便宜，推荐测试用
                .modelName(aiConfig.getModelId())
                // 温度，与输出的随机度有关 参考值 0.1-1.0
                .temperature(aiConfig.getTemperature())
                // 限制采样时选择的概率质量范围 参考值 0.9-1.0
                .topP(aiConfig.getSimilarityTopP())
                // 最大输出token数量 参考值 1000-10000
                .maxTokens(aiConfig.getMaxContextMsgs())
                .build();

        // 创建一个流式响应处理器
        StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<>() {
            @Override
            public void onNext(String s) {
                log.info("***AI对话 onNext: {}", s);
            }

            @Override
            public void onComplete(Response response) {
                log.info("***AI对话 onComplete: {}", response.toString());
                // 停止倒计时
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                log.error("[❌ 错误] : {}", error.getMessage());
            }
        };

        // 这里可以换成其它任何问题
        model.generate("你是谁？", handler);

        // 等待响应完成，最多等待60秒
        latch.await(60, TimeUnit.SECONDS);
    }

    /**
     * 多轮对话测试
     */
    @Test
    void testLangChain4jMultiRound() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        AiConfig aiConfig = aiConfigMapper.selectByPrimaryKey(1);

        // 构建模型对象，使用百炼 OpenAI 兼容模式
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                // 这里也可以直接填你的apiKey
                .apiKey(aiConfig.getApiKey())
                // 百炼域名地址 https://dashscope.aliyuncs.com/compatible-mode/v1
                .baseUrl(aiConfig.getApiDomain())
                // qwen-plus、qwen-max、qwen-turbo 等
                // qwen-turbo 比较便宜，推荐测试用
                .modelName(aiConfig.getModelId())
                // 温度，与输出的随机度有关 参考值 0.1-1.0
                .temperature(aiConfig.getTemperature())
                // 限制采样时选择的概率质量范围 参考值 0.9-1.0
                .topP(aiConfig.getSimilarityTopP())
                // 最大输出token数量 参考值 1000-10000
                .maxTokens(aiConfig.getMaxContextMsgs())
                .build();

        // 创建一个流式响应处理器
        StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<>() {
            @Override
            public void onNext(String s) {
                log.info("***AI对话 onNext: {}", s);
            }

            @Override
            public void onComplete(Response response) {
                log.info("***AI对话 onComplete: {}", response.toString());
                // 停止倒计时
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                log.error("[❌ 错误] : {}", error.getMessage());
            }
        };

        // TODO 这里是异步的，添加先后顺序
        // 这里可以换成其它任何问题
        model.generate("我是losgai，请记住我", handler);

        model.generate("我是谁？", handler);

        // 等待响应完成，最多等待60秒
        latch.await(60, TimeUnit.SECONDS);
    }

    @Test
    public void ragStoreWithSpringAI() throws IOException {
        // 文档，由2部分组成：元数据和metaData，用于检索和过滤
        List<Document> documents = List.of(
                new Document("怪物猎人荒野在2025年10月31日的在线玩家峰值为15万", Map.of("title", "荒野")),
                new Document("怪物猎人世界在2025年10月31日的在线玩家峰值为12万", Map.of("title", "世界")),
                new Document("怪物猎人崛起在2025年10月31日的在线玩家峰值为7万", Map.of("title", "崛起")));

        ElasticsearchIndexUtils.createIndex("test_vector", esClient);
        vectorStore.add(documents);
    }

    @Test
    public void ragRetrieveWithSpringAI() {
        List<Document> mh = vectorStore.similaritySearch("怪物猎人荒野");

        log.info("相似度搜索结果：{}", mh);
    }

}