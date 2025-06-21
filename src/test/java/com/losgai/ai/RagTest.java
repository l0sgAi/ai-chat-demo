package com.losgai.ai;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.losgai.ai.aiservice.Assistant;
import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.util.OpenAiModelBuilder;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RagTest {


    @Test
    public void startEmbedding() throws IOException {
        // 也可以从数据库读取API Key
        AiConfig aiConfig = new AiConfig();

        aiConfig.setApiKey("sk...");
        aiConfig.setApiDomain("https://dashscope.aliyuncs.com/compatible-mode/v1");
        aiConfig.setModelId("qwen-turbo-latest");
        aiConfig.setTemperature(0.9);
        aiConfig.setSimilarityTopK(0.2);
        aiConfig.setMaxContextMsgs(2048);
        aiConfig.setSimilarityTopP(1.0);

        // 构建嵌入模型，使用openAI标准
        OpenAiEmbeddingModel model = OpenAiEmbeddingModel.builder()
                // 这里换成自己的API Key
                .apiKey(aiConfig.getApiKey())
                // 即为 "https://dashscope.aliyuncs.com/compatible-mode/v1"
                .baseUrl(aiConfig.getApiDomain())
                // 需要使用QWEN的文本向量模型 支持的维度2048、1536、1024（默认）、768、512、256、128、64
                .modelName("text-embedding-v4")
                .dimensions(1024) // 直接指定向量维度
                .build();

        // 检查模型的生成维数
        log.info("model: {}", model.dimension());

        // 构建测试用文本
        TextSegment game1 = TextSegment.from(
                "电子游戏:METAL GEAR SOLID V: THE PHANTOM PAIN由小岛秀夫制作，但是由于黑心企业KONAMI的介入，" +
                        "后期开发资金不足，是一个半成品，" +
                        "即便如此，它也在国际上获得了良好的声誉，获奖无数。" +
                        "在2025年6月5日，它的在线人数为993人。",
                Metadata.from("gameName", "METAL GEAR SOLID V: THE PHANTOM PAIN"));
        Embedding embedding1 = model.embed(game1.text()).content();

        TextSegment game2 = TextSegment.from(
                "电子游戏:MONSTER HUNTER: WILDS在刚发售的时候销量还不错，但是后期的发展却不太理想，"
                        + "在2025年6月5日，即使刚刚更新了游戏内容不久，同时在线人数只有1.2万左右。",
                Metadata.from("gameName", "MONSTER HUNTER: WILDS"));
        Embedding embedding2 = model.embed(game2.text()).content();

        /*
         * 显然，上面的这种方法不适用于更大的数据集，
         * 因为这个数据存储系统会把所有内容都存储在内存中，
         * 而我们的服务器内存有限。因此，我们可以将嵌入存储到 Elasticsearch 中，
         * 现在的解决方案只是测试用
         */

        // 初始化Elasticsearch实例
        // 1. 创建 RestClient（无认证、无 SSL）
        // TODO 如果在设置里打开了认证，需要配置认证信息
        RestClient restClient = RestClient.builder(
                // 这里换成自己的ES服务器地址，如果是本地部署，直接localhost即可
                new HttpHost("localhost", 9200)).build();

        // 2. 使用 Jackson 映射器创建 Transport 层
        RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // 3. 创建 Elasticsearch Java 客户端
        ElasticsearchClient client = new ElasticsearchClient(transport);

        // 4. 使用ES储存文本向量
        ElasticsearchEmbeddingStore store = ElasticsearchEmbeddingStore.builder()
                .indexName("games") // 这里换成自己的ES索引名
                .restClient(restClient)
                .build();

        // 5. 向ES存储文本向量
        store.add(embedding1, game1);
        store.add(embedding2, game2);

        // 搜索相似向量-搜索
        String question = "电子游戏:MONSTER HUNTER: WILDS在6月5日的同时在线人数";
        Embedding questionAsVector = model.embed(question).content();
        // 搜索相似向量-搜索结果
        EmbeddingSearchResult<TextSegment> result = store.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(questionAsVector)
                        .build());

        // 打印匹配结果
        result.matches().forEach(m -> log.info("{} - score [{}]",
                m.embedded().metadata().getString("gameName"), m.score()));
    }

    @Test
    public void startQueryWithRAG() throws InterruptedException {
        // 也可以从数据库读取API Key
        AiConfig aiConfig = new AiConfig();

        aiConfig.setApiKey("sk-...");
        aiConfig.setApiDomain("https://dashscope.aliyuncs.com/compatible-mode/v1");
        aiConfig.setModelId("qwen-turbo-latest");
        aiConfig.setTemperature(0.9);
        aiConfig.setSimilarityTopK(0.2);
        aiConfig.setMaxContextMsgs(2048);
        aiConfig.setSimilarityTopP(1.0);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        // 构建嵌入模型，使用openAI标准
        OpenAiEmbeddingModel model = OpenAiEmbeddingModel.builder()
                // 这里换成自己的API Key
                .apiKey("sk-...")
                // 即为 "https://dashscope.aliyuncs.com/compatible-mode/v1"
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                // 需要使用QWEN的文本向量模型 支持的维度2048、1536、1024（默认）、768、512、256、128、64
                .modelName("text-embedding-v4")
                .dimensions(1024) // 直接指定向量维度
                .build();

        // 初始化Elasticsearch实例
        // 1. 创建 RestClient（无认证、无 SSL）
        // TODO 如果在设置里打开了认证，需要配置认证信息
        RestClient restClient = RestClient.builder(
                // 这里换成自己的ES服务器地址，如果是本地部署，直接localhost即可
                new HttpHost("localhost", 9200)).build();

        // 2. 使用 Jackson 映射器创建 Transport 层
        RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // 3. 创建 Elasticsearch Java 客户端
        ElasticsearchClient client = new ElasticsearchClient(transport);

        // 4. 获取ES中对应的索引储存
        ElasticsearchEmbeddingStore store = ElasticsearchEmbeddingStore.builder()
                .indexName("games") // 这里换成自己的ES索引名
                .restClient(restClient)
                .build();

        // 5. 创建内容检索器
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(model)
                .maxResults(5)
                .minScore(0.75)
                .build();

        OpenAiStreamingChatModel myChatModelStream = OpenAiModelBuilder.fromAiConfigByLangChain4j(aiConfig);

        Assistant assistantStream = AiServices.builder(Assistant.class)
                .streamingChatLanguageModel(myChatModelStream) // 如阿里百炼的 ChatModel 封装
                .contentRetriever(contentRetriever) // 你刚刚创建好的带 EmbeddingStore 的 ContentRetriever
                .build();

        // 给出答案
        String question = "请问在2025年6月5日，MONSTER HUNTER: WILDS和METAL GEAR SOLID V: THE PHANTOM PAIN的在线人数分别是多少？";
        Flux<String> responseFlux = assistantStream.chat(question);
        // 订阅 Flux 实现流式输出（控制台输出或 SSE 推送）
        responseFlux.subscribe(
                token -> log.info("输出token:{}", token), // 每个token响应
                error -> {
                    log.error("出错：", error);
                    countDownLatch.countDown(); // 停止倒计时
                }, // 错误处理
                () -> {// 流结束
                    log.info("\n回答完毕！");
                    countDownLatch.countDown(); // 停止倒计时
                });

        // 阻塞主线程最多60s 等待结果
        countDownLatch.await(60, TimeUnit.SECONDS);

    }
}