# 阿里百炼平台整合QWEN与langchain4j

*阿里的`QWEN3`是一个性价比比较高的大模型，非常适合用来练手，本文将会教会大家去使用百炼平台整合简单的大模型应用到您的应用程序中，使用`langchain4j`。本人也是一个初学者，难免会有一些问题和错误，还请大家指正。*

**项目地址:[ai-chat-demo](https://github.com/l0sgAi/ai-chat-demo)**

### 阅读本文前应该有的知识基础

- Java基础

- Java Web基础

- 数据库基础

- 包管理 (Maven、gradle等)

- Spring基础、SSM整合、SpringBoot等

---

### 文本流式对话输出

> 文本流式对话是一个非常常见的大模型应用，可以让用户的阅读和`ai`输出同时进行，特别是当输出内容很长时，可以极大优化用户体验。

在阿里云的百炼官方文档，我只找到了使用java进行非流式对话的示例。其实使用`langchain4j`，可以十分方便地调用你的API去进行流式对话。

##### 前置准备

1. 登录你的[百炼控制台](https://bailian.console.aliyun.com/?tab=model#/api-key); 如果没有注册，请先注册新账号，有免费额度可以用。

2. 获取`API KEY` ; 官方文档建议使用系统变量，按照官方文档教程即可，但是如果你需要对使用的大模型进行配置，则需要建一个配置表，通过数据库传入大模型配置参数，这个后面会详细介绍。官方文档：[百炼控制台](https://bailian.console.aliyun.com/?tab=doc#/doc/)

##### 开始测试

- 进入你的`SpringBoot`项目，新建一个测试

- 引入相关`maven`依赖，如果需要其它版本，请到[Maven仓库](https://mvnrepository.com/)中搜索

```xml
<!-- OpenAI compatible API (支持 Qwen 百炼 OpenAI 模式) -->
<!-- https://mvnrepository.com/artifact/dev.langchain4j/langchain4j-open-ai -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.35.0</version>
</dependency>
```

- 开始编写测试，正常配置的话应该会在控制台分段输出ai回复

```java
import com.losgai.ai.entity.AiConfig;
import com.losgai.ai.mapper.AiConfigMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
public class AiApplicationTests {

    @Autowired
    private AiConfigMapper aiConfigMapper;

    /** 通过数据库获取配置，初始化模型进行输出
     * 使用langchain4j*/
    @Test
    void testLangChain4j() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        AiConfig aiConfig = aiConfigMapper.selectByPrimaryKey(1);

        // 构建模型对象，使用百炼 OpenAI 兼容模式
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(aiConfig.getApiKey()) // 这里也可以直接填你的apiKey
                .baseUrl(aiConfig.getApiDomain()) // 百炼兼容地址
                .modelName(aiConfig.getModelId()) // qwen-turbo比较便宜，测试用
                // qwen-plus、qwen-max、qwen-turbo 等
                .temperature(aiConfig.getSimilarityTopK()) // 温度，与输出的随机度有关
                .topP(aiConfig.getSimilarityTopP()) // 限制采样时选择的概率质量范围
                .maxTokens(aiConfig.getMaxContextMsgs()) // 最大输出token数量
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

}
```

**测试结果**

```bash
2025-06-01T16:09:45.339+08:00  INFO 22184 --- [ai] [           main] com.alibaba.druid.pool.DruidDataSource   : {dataSource-1} inited
2025-06-01T16:09:51.501+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 
2025-06-01T16:09:51.503+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 我是
2025-06-01T16:09:51.503+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 通
2025-06-01T16:09:51.510+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 义
2025-06-01T16:09:51.525+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 千
2025-06-01T16:09:51.542+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 问，是阿里巴巴
2025-06-01T16:09:51.562+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 集团旗下的通义
2025-06-01T16:09:51.607+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 实验室自主研发的超
2025-06-01T16:09:51.660+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 大规模语言模型。
2025-06-01T16:09:51.762+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 我的中文名是
2025-06-01T16:09:51.781+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 通义千问
2025-06-01T16:09:51.821+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: ，英文名是
2025-06-01T16:09:51.877+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: Qwen。我
2025-06-01T16:09:51.932+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 能够回答问题、
2025-06-01T16:09:51.992+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 创作文字、编程
2025-06-01T16:09:52.077+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 、逻辑推理等多种
2025-06-01T16:09:52.097+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 任务，旨在为
2025-06-01T16:09:52.154+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 用户提供全面、准确
2025-06-01T16:09:52.210+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 、有用的信息和服务
2025-06-01T16:09:52.263+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 。如果你有任何问题
2025-06-01T16:09:52.335+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 或需要帮助，
2025-06-01T16:09:52.413+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 欢迎随时告诉我！
2025-06-01T16:09:52.464+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onNext: 
2025-06-01T16:09:52.466+08:00  INFO 22184 --- [ai] [liyuncs.com/...] com.losgai.ai.AiApplicationTests         : ***AI对话 onComplete: Response { content = AiMessage { text = "我是通义千问，是阿里巴巴集团旗下的通义实验室自主研发的超大规模语言模型。我的中文名是通义千问，英文名是Qwen。我能够回答问题、创作文字、编程、逻辑推理等多种任务，旨在为用户提供全面、准确、有用的信息和服务。如果你有任何问题或需要帮助，欢迎随时告诉我！" toolExecutionRequests = null }, tokenUsage = TokenUsage { inputTokenCount = 15, outputTokenCount = 72, totalTokenCount = 87 }, finishReason = STOP, metadata = {} }
2025-06-01T16:09:52.503+08:00  INFO 22184 --- [ai] [ionShutdownHook] com.alibaba.druid.pool.DruidDataSource   : {dataSource-1} closing ...
2025-06-01T16:09:52.516+08:00  INFO 22184 --- [ai] [ionShutdownHook] com.alibaba.druid.pool.DruidDataSource   : {dataSource-1} closed

进程已结束，退出代码为 0
```

测试成功之后，就可以开始封装自己的AI流式对话方法了。

**平台提供多种模型，模型列表：[模型列表_大模型服务平台百炼(Model Studio)-阿里云帮助中心](https://help.aliyun.com/zh/model-studio/getting-started/models)**

##### 封装使用

> 开始之前，我们需要一些准备工作，创建一些数据表，我这里使用的是`Mysql8`, 大家也可以选择任意自己喜欢的数据库，具体的建表我放在文末。如果你还使用`Mybatis` (推荐), 可以使用`mybatis-generator`或`mybatisX-generator`生成对应的实体类和对应的增删改查代码。

---

**根据读取的配置构建模型**

```java
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import com.losgai.ai.chat.entity.AiConfig;

public class OpenAiModelBuilder {

    public static OpenAiStreamingChatModel fromAiConfig(AiConfig config) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getApiDomain()) // 如 https://dashscope.aliyuncs.com/compatible-mode/v1
                .modelName(config.getModelId()) // 如 qwen-turbo-latest
                .temperature(config.getSimilarityTopK()) // 可从 config 拓展配置
                .topP(config.getSimilarityTopP())
                .maxTokens(config.getMaxContextMsgs())
                .apiKey(config.getApiKey())
                .build();
    }
}
```

**统一封装构建**

```java
import com.losgai.ai.entity.AiConfig;
import com.losgai.ai.util.OpenAiModelBuilder;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class AiChatMessageService {

    public ChatSession chatMessageStream(AiConfig config, String userMessage) {
        OpenAiStreamingChatModel model = OpenAiModelBuilder.fromAiConfigByLangChain4j(config);
        return new ChatSession(model, userMessage);
    }

    public static class ChatSession {
        private final OpenAiStreamingChatModel model;
        private final String question;

        private Consumer<String> onNextConsumer;
        private Consumer<Response<AiMessage>> onCompleteConsumer;
        private Consumer<Throwable> onErrorConsumer;

        public ChatSession(OpenAiStreamingChatModel model, String question) {
            this.model = model;
            this.question = question;
        }

        public ChatSession onNext(Consumer<String> consumer) {
            this.onNextConsumer = consumer;
            return this;
        }

        public ChatSession onComplete(Consumer<Response<AiMessage>> consumer) {
            this.onCompleteConsumer = consumer;
            return this;
        }

        public ChatSession onError(Consumer<Throwable> consumer) {
            this.onErrorConsumer = consumer;
            return this;
        }

        public void start() {
            model.generate(question, new StreamingResponseHandler<>() {
                @Override
                public void onNext(String token) {
                    if (onNextConsumer != null) {
                        onNextConsumer.accept(token);
                    }
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    if (onCompleteConsumer != null) {
                        onCompleteConsumer.accept(response);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    if (onErrorConsumer != null) {
                        onErrorConsumer.accept(throwable);
                    }
                }
            });
        }
    }
}
```

##### 整合流式输出

**线程池配置**

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class ThreadPoolConfig {

    @Bean("aiWorkerExecutor")
    public Executor aiWorkerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4); // TODO 调整线程池设置
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ai-worker-");
        // 默认拒绝策略，队列满了直接抛异常
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

}
```

**SEE连接管理**

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一处理sse连接
 */
@Component
@Slf4j
public class SseEmitterManager {
    // 支持的同时在线人数=SESSION_LIMIT-1 有一个监控sse
    private static final int SESSION_LIMIT = 101;

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 将对话请求加入队列
     */
    public boolean addEmitter(String sessionId, SseEmitter emitter) {
        if (emitterMap.size() < SESSION_LIMIT) {
            emitterMap.put(sessionId, emitter);
            // 推流当前线程数
            this.notifyThreadCount();
            return true;
        }
        return false;
    }

    /**
     * 获取Map中的sse连接
     */
    public SseEmitter getEmitter(String sessionId) {
        return emitterMap.get(sessionId);
    }

    public void removeEmitter(String sessionId) {
        emitterMap.remove(sessionId);
        // 推流当前线程数
        this.notifyThreadCount();
    }

    public boolean isOverLoad() {
        return emitterMap.size() >= SESSION_LIMIT;
    }

    public int getEmitterCount() {
        return emitterMap.size();
    }

    /**
     * 获取线程监控实例
     */
    public void addThreadMonitor() {
        // 添加一个线程监控
        emitterMap.put("thread-monitor", new SseEmitter(0L));
    }

    /**
     * 手动发送消息，通知当前占用的线程数
     */
    public void notifyThreadCount() {
        SseEmitter sseEmitter = emitterMap.get("thread-monitor");
        try {
            if (emitterMap.containsKey("thread-monitor")) {
                sseEmitter.send(this.getEmitterCount());
            }else {
                addThreadMonitor();
                sseEmitter = emitterMap.get("thread-monitor");
                sseEmitter.send(this.getEmitterCount());
            }
        } catch (IOException e) {
            sseEmitter.completeWithError(e);
            emitterMap.remove("thread-monitor"); // 清除失效连接
        }
    }

    /**
     * 将sse连接全部关闭
     */
    public void closeAll() {
        for (SseEmitter emitter : emitterMap.values()) {
            emitter.complete();
        }
        emitterMap.clear();
    }

}
```

**在Service层使用**

```java
import com.losgai.ai.dto.AiChatParamDTO;
import com.losgai.ai.entity.AiConfig;
import com.losgai.ai.entity.AiMessagePair;
import com.losgai.ai.enums.AiMessageStatusEnum;
import com.losgai.ai.global.SseEmitterManager;
import com.losgai.ai.mapper.AiConfigMapper;
import com.losgai.ai.mapper.AiMessagePairMapper;
import com.losgai.ai.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatServiceImpl implements AiChatService {


    private final AiMessagePairMapper aiMessagePairMapper;

    private final SseEmitterManager emitterManager;

    private final AiChatMessageService aiChatMessageService;

    private final AiConfigMapper aiConfigMapper;

    @Override
    @Async("aiWorkerExecutor")
    public CompletableFuture<Boolean> handleQuestionAsync(AiChatParamDTO aiChatParamDTO, String sessionId) {
        if (emitterManager.isOverLoad()) return CompletableFuture.completedFuture(false);
        // 获取会话id对应的sseEmitter
        SseEmitter emitter = emitterManager.getEmitter(sessionId);
        // 没有则先创建一个sseEmitter
        if (emitter == null) {
            if (emitterManager.addEmitter(sessionId, new SseEmitter(0L))) {
                emitter = emitterManager.getEmitter(sessionId);
            } else {
                // 创建失败，一般是由于队列已满，直接返回false
                return CompletableFuture.completedFuture(false);
            }
        }
        // 先发送一次队列人数通知
        emitterManager.notifyThreadCount();
        // 最终指向的emitter对象
        SseEmitter finalEmitter = emitter;
        StringBuffer sb = new StringBuffer();
        // 开始对话，返回token流
        // 封装插入的信息对象
        AiMessagePair aiMessagePair = new AiMessagePair();
        aiMessagePair.setSseSessionId(sessionId);
        aiMessagePair.setSessionId(aiChatParamDTO.getChatSessionId());
        aiMessagePair.setModelUsed(aiChatParamDTO.getModelId());
        AiConfig aiConfig = aiConfigMapper.selectByPrimaryKey(aiChatParamDTO.getModelId());
        if (aiConfig == null || aiConfig.getModelType() != 0) {
            // 返回的配置 不是大模型，直接返回false
            return CompletableFuture.completedFuture(false);
        }
        // 标志位，判断是否更新成功，防止重复插入
        AtomicBoolean updated = new AtomicBoolean(false);
        try {
            aiChatMessageService.chatMessageStream(aiConfig, aiChatParamDTO.getQuestion())
                    .onNext(token -> {
                        if (updated.get()) {
                            log.warn("===>已经取消ai生成");
                            return; // 已取消，不再处理
                        }
                        try {
                            sb.append(token);
//                            log.info("当前token：{}",token);
                            // HTML 换行符转义：转换成<br>
                            token = token.replace("\n", "<br>");
                            // HTML 空格转义：转换成 
                            token = token.replace(" ", " ");
                            finalEmitter.send(SseEmitter.event().data(token));
                            // log.info("当前段数据:{}", token);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .onComplete(response -> {
                        finalEmitter.complete();
                        emitterManager.removeEmitter(sessionId); // 只在流结束后移除
//                        log.info("最终拼接的数据:{} | token使用:{}", sb,response.tokenUsage().totalTokenCount());
                        tryUpdateMessage(aiMessagePair,
                                AiMessageStatusEnum.FINISHED.getCode(),
                                sb.toString(),
                                response.tokenUsage().totalTokenCount(),
                                updated);
                    })
                    .onError(e -> {
                        log.error("ai对话|流式输出报错:{}", e.getMessage());
                        try {
                            finalEmitter.send(SseEmitter.event().data("[错误] " + e.getMessage()));
                            finalEmitter.completeWithError(e);
                        } catch (IOException ioException) {
                            finalEmitter.completeWithError(ioException);
                        }
                        finalEmitter.complete();
                        emitterManager.removeEmitter(sessionId); // 出错时也移除
                        tryUpdateMessage(aiMessagePair,
                                AiMessageStatusEnum.STOPPED.getCode(),
                                sb.toString(),
                                null,
                                updated);
                    })
                    .start();
        } catch (Exception e) {
            log.error("处理ai对话报错:{}", e.getMessage());
            finalEmitter.completeWithError(e);
            emitterManager.removeEmitter(sessionId); // 捕获到异常时移除
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Boolean handleQuestion(AiChatParamDTO aiChatParamDTO, String sessionId) {
        if (emitterManager.isOverLoad()) return false;
        // 获取会话id对应的sseEmitter
        SseEmitter emitter = emitterManager.getEmitter(sessionId);
        // 没有则先创建一个sseEmitter
        if (emitter == null) {
            if (emitterManager.addEmitter(sessionId, new SseEmitter(0L))) {
                emitter = emitterManager.getEmitter(sessionId);
            } else {
                // 创建失败，一般是由于队列已满，直接返回false
                return false;
            }
        }
        // 先发送一次队列人数通知
        emitterManager.notifyThreadCount();
        // 最终指向的emitter对象
        SseEmitter finalEmitter = emitter;
        StringBuffer sb = new StringBuffer();
        // 开始对话，返回token流
        // 封装插入的信息对象
        AiMessagePair aiMessagePair = new AiMessagePair();
        aiMessagePair.setSseSessionId(sessionId);
        aiMessagePair.setSessionId(aiChatParamDTO.getChatSessionId());
        aiMessagePair.setModelUsed(aiChatParamDTO.getModelId());
        AiConfig aiConfig = aiConfigMapper.selectByPrimaryKey(aiChatParamDTO.getModelId());
        if (aiConfig == null || aiConfig.getModelType() != 0) {
            // 返回的配置 不是大模型，直接返回false
            return false;
        }
        // 标志位，判断是否更新成功，防止重复插入
        AtomicBoolean updated = new AtomicBoolean(false);
        try {
            aiChatMessageService.chatMessageStream(aiConfig, aiChatParamDTO.getQuestion())
                    .onNext(token -> {
                        if (updated.get()) {
                            log.warn("===>已经取消ai生成");
                            return; // 已取消，不再处理
                        }
                        try {
                            // 换行符转义：如果token以换行符为结尾，转换成<br>
                            sb.append(token);
                            token = token.replace("\n", "<br>");
                            token = token.replace(" ", " ");
                            finalEmitter.send(SseEmitter.event().data(token));
                            // log.info("当前段数据:{}", token);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .onComplete(response -> {
                        finalEmitter.complete();
                        emitterManager.removeEmitter(sessionId); // 只在流结束后移除
                        log.info("最终拼接的数据:{}", sb);
                        tryUpdateMessage(aiMessagePair,
                                AiMessageStatusEnum.FINISHED.getCode(),
                                sb.toString(),
                                response.tokenUsage().totalTokenCount(),
                                updated);
                    })
                    .onError(e -> {
                        log.error("ai对话|流式输出报错:{}", e.getMessage());
                        try {
                            finalEmitter.send(SseEmitter.event().data("[错误] " + e.getMessage()));
                            finalEmitter.completeWithError(e);
                        } catch (IOException ioException) {
                            finalEmitter.completeWithError(ioException);
                        }
                        finalEmitter.complete();
                        emitterManager.removeEmitter(sessionId); // 出错时也移除
                        tryUpdateMessage(aiMessagePair,
                                AiMessageStatusEnum.STOPPED.getCode(),
                                sb.toString(),
                                null,
                                updated);
                    })
                    .start();
        } catch (Exception e) {
            log.error("处理ai对话报错:{}", e.getMessage());
            finalEmitter.completeWithError(e);
            emitterManager.removeEmitter(sessionId); // 捕获到异常时移除
        }
        return true;
    }

    // ai回答推流sse
    @Override
    public SseEmitter getEmitter(String sessionId) {
        // 获取对应sessionId的sseEmitter
        SseEmitter emitter = emitterManager.getEmitter(sessionId);
        if (emitter != null) {
            emitter.onCompletion(() -> emitterManager.removeEmitter(sessionId));
            emitter.onTimeout(() -> emitterManager.removeEmitter(sessionId));
            return emitter;
        }
        return null;
    }

    /**
     * 尝试插入消息的方法
     */
    private void tryUpdateMessage(AiMessagePair message,
                                  int status,
                                  String content,
                                  Integer tokenUsed,
                                  AtomicBoolean flag) {
        if (flag.compareAndSet(false, true)) {
            message.setStatus(status);
            message.setAiContent(content);
            message.setTokens(tokenUsed);
            message.setResponseTime(Date.from(Instant.now()));
            aiMessagePairMapper.updateBySseIdSelective(message);
        }
    }

}
```

**如果使用虚拟线程 (JDK21+)**

```java
@Override
public CompletableFuture<Boolean> handleQuestionAsyncByVirtualThread(AiChatParamDTO aiChatParamDTO, String sessionId) {
    return CompletableFuture.supplyAsync(() -> {
        if (emitterManager.isOverLoad()) return false;
        SseEmitter emitter = emitterManager.getEmitter(sessionId);
        if (emitter == null) {
            if (emitterManager.addEmitter(sessionId, new SseEmitter(0L))) {
                emitter = emitterManager.getEmitter(sessionId);
            } else {
                return false;
            }
        }
        emitterManager.notifyThreadCount();

        SseEmitter finalEmitter = emitter;
        StringBuffer sb = new StringBuffer();
        AiMessagePair aiMessagePair = new AiMessagePair();
        aiMessagePair.setSseSessionId(sessionId);
        aiMessagePair.setSessionId(aiChatParamDTO.getChatSessionId());
        aiMessagePair.setModelUsed(aiChatParamDTO.getModelId());

        AiConfig aiConfig = aiConfigMapper.selectByPrimaryKey(aiChatParamDTO.getModelId());
        if (aiConfig == null || aiConfig.getModelType() != 0) {
            return false;
        }

        AtomicBoolean updated = new AtomicBoolean(false);
        try {
            aiChatMessageService.chatMessageStream(aiConfig, aiChatParamDTO.getQuestion())
                    .onNext(token -> {
                        if (updated.get()) {
                            log.warn("===>已经取消ai生成");
                            return;
                        }
                        try {
                            sb.append(token);
                            log.info("当前token：{}", token);
                            token = token.replace("\n", "<br>");
                            token = token.replace(" ", "&nbsp;");
                            finalEmitter.send(SseEmitter.event().data(token));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .onComplete(response -> {
                        finalEmitter.complete();
                        log.info("最终拼接的数据:{} | token使用:{}", sb,response.tokenUsage().totalTokenCount());
                        emitterManager.removeEmitter(sessionId);
                        tryUpdateMessage(aiMessagePair,
                                AiMessageStatusEnum.FINISHED.getCode(),
                                sb.toString(),
                                response.tokenUsage().totalTokenCount(),
                                updated);
                    })
                    .onError(e -> {
                        log.error("ai对话|流式输出报错:{}", e.getMessage());
                        try {
                            finalEmitter.send(SseEmitter.event().data("[错误] " + e.getMessage()));
                            finalEmitter.completeWithError(e);
                        } catch (IOException ioException) {
                            finalEmitter.completeWithError(ioException);
                        }
                        finalEmitter.complete();
                        emitterManager.removeEmitter(sessionId);
                        tryUpdateMessage(aiMessagePair,
                                AiMessageStatusEnum.STOPPED.getCode(),
                                sb.toString(),
                                null,
                                updated);
                    })
                    .start();
        } catch (Exception e) {
            log.error("处理ai对话报错:{}", e.getMessage());
            finalEmitter.completeWithError(e);
            emitterManager.removeEmitter(sessionId);
        }

        return true;
    }, Executors.newVirtualThreadPerTaskExecutor());
}
```

**整体思路**

* 包括三张表 (会话、问答对、配置)，从配置表查大模型配置项进行初始化，用户发送消息后，立刻往数据库插入问答对 (问答对属于一个会话记录) 。`AI`服务在流式调用完成后，再对其它属性进行更新 (包括状态、`token`数等)。

* 使用异步方法调用对话服务，发送`POST`请求到`/chat/send`，会返回一个`sessionId` (`UUID`+时间戳), 并创建一个`sseEmitter`, 通过`SSE`给前端推流, 使用另一个接口获取这个`SSE`即可: `GET chat/stream/{sessionId}`。

* 对于前端，可以使用[marked.js](https://github.com/markedjs/marked)+ [highlight.js](https://highlightjs.org) 进行渲染, 结合[github-markdown-css](https://github.com/sindresorhus/github-markdown-css)以获得更加美观的输出! 大家有空可以试一试。

---

# 总结

本文讨论了使用`langchain4j`整合`SpringBoot`应用与大模型的方法，本人是初学者，难免有很多不足和错误，有任何问题和建议，欢迎在评论区或[GitHub讨论区](https://github.com/l0sgAi/ai-chat-demo/issues)留言！

---

# 相关建表

### AI会话表

```sql
-- 会话表
CREATE TABLE ai_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键，自增ID',
    title VARCHAR(255) DEFAULT NULL COMMENT '对话主题',
    is_favorite BOOLEAN DEFAULT FALSE COMMENT '是否收藏',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_message_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后对话时间',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    model_id INT DEFAULT NULL COMMENT '使用的模型ID',
    tags VARCHAR(255) DEFAULT NULL COMMENT '标签，逗号分隔',
    summary TEXT DEFAULT NULL COMMENT '对话摘要'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话记录表';
```

### 问答对表

```sql
CREATE TABLE ai_message_pair (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键，自增ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    sse_session_id VARCHAR(64) NOT NULL COMMENT 'SSE会话ID',
    user_content TEXT NOT NULL COMMENT '用户提问内容',
    ai_content MEDIUMTEXT DEFAULT NULL COMMENT 'AI回复内容',
    model_used INT DEFAULT NULL COMMENT '使用模型id',
    status TINYINT(4) DEFAULT 0 COMMENT '状态：0-生成中 1-完成 2-中断',
    tokens INT UNSIGNED DEFAULT NULL COMMENT '本轮消耗的Token',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '用户提问时间',
    response_time DATETIME DEFAULT NULL COMMENT 'AI回复完成时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一轮问答记录表';
-- 参考索引
CREATE UNIQUE INDEX idx_sse_session_id ON ai_message_pair(sse_session_id);
```

### ai配置表

```sql
CREATE TABLE ai_config (
  id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键，自增ID',
  display_name VARCHAR(64) NOT NULL COMMENT '显示名称',
  api_domain VARCHAR(255) NOT NULL COMMENT 'API域名',
  model_name VARCHAR(128) NOT NULL COMMENT '模型名称',
  model_type TINYINT NOT NULL COMMENT '模型类型：0-大模型，1-文本向量，2-视觉模型',
  model_id VARCHAR(128) NOT NULL COMMENT '模型ID',
  api_key VARCHAR(255) NOT NULL COMMENT 'API密钥',
  max_context_msgs INT DEFAULT 4096 COMMENT '上下文最大消息数',
  similarity_top_p FLOAT DEFAULT 1.0 COMMENT '相似度TopP',
  similarity_top_k FLOAT DEFAULT 0.1 COMMENT '相似度TopK',
  is_default TINYINT(1) DEFAULT 0 COMMENT '是否为默认模型(是0/否1)',
  case_tags VARCHAR(255) DEFAULT NULL COMMENT '标签',
  case_brief VARCHAR(255) DEFAULT NULL COMMENT '简介',
  case_remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='AI配置信息表';
```