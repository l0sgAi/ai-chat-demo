# 阿里百炼平台整合QWEN与langchain4j

*阿里的`QWEN3`是一个性价比比较高的大模型，非常适合用来练手，本文将会教会大家去使用百炼平台整合简单的大模型应用到您的应用程序中，使用`langchain4j`。本人也是一个初学者，难免会有一些问题和错误，还请大家指正。*

### 阅读本文前应该有的知识基础

- Java基础

- Java Web基础

- Spring基础

- 数据库基础

- 包管理 (Maven、gradle等)

---

### 文本流式对话输出

> 文本流式对话是一个非常常见的大模型应用，可以让用户的阅读和`ai`输出同时进行，特别是当输出内容很长时，可以极大优化用户体验。

在阿里云的百炼官方文档，我只找到了使用java进行非流式对话的示例。其实使用`langchain4j`，可以十分方便地调用你的API去进行流式对话。

##### 前置准备

1. 登录你的[百炼控制台](https://bailian.console.aliyun.com/?tab=model#/api-key); 如果没有注册，请先注册新账号，有免费额度可以用。

2. 获取`API KEY` ; 官方文档建议使用系统变量，按照官方文档教程即可，但是如果你需要对使用的大模型进行配置，则需要建一个配置表，通过数据库传入大模型配置参数，这个后面会详细介绍。官方文档：[百炼控制台](https://bailian.console.aliyun.com/?tab=doc#/doc/)

##### 开始测试

- 进入你的`SpringBoot`项目，新建一个测试（或者直接使用一个主函数也行）

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
        StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<AiMessage>() {
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

**在Service层使用**

```java
import com.sinnren.ffa.email.dto.AiChatParamDTO;
import com.sinnren.ffa.email.entity.AiMessagePair;
import com.sinnren.ffa.email.enums.AiMessageStatusEnum;
import com.sinnren.ffa.email.global.SseEmitterManager;
import com.sinnren.ffa.email.mapper.AiConfigMapper;
import com.sinnren.ffa.email.mapper.AiMessagePairMapper;
import com.sinnren.ffa.email.service.AiChatService;
import com.sinnren.llm.biz.service.AiChatMessageService;
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

    // AI对话服务
    private final AiChatMessageService aiChatMessageService;

    private final AiMessagePairMapper aiMessagePairMapper;

    private final SseEmitterManager emitterManager;

    private final AiConfigMapper aiConfigMapper;

    @Override
    @Async("aiWorkerExecutor")
    public CompletableFuture<Boolean> handleQuestionAsync(AiChatParamDTO aiChatParamDTO, String sessionId) {
        if (emitterManager.isOverLoad()) return CompletableFuture.completedFuture(false);
        // 获取会话id对应的sseEmitter
        SseEmitter emitter = emitterManager.getEmitter(sessionId);
        // 先发送一次队列人数通知
        emitterManager.notifyThreadCount();
        // 没有则先创建一个sseEmitter
        if (emitter == null) {
            if (emitterManager.addEmitter(sessionId, new SseEmitter(0L))) {
                emitter = emitterManager.getEmitter(sessionId);
            } else {
                // 创建失败，一般是由于队列已满，直接返回false
                return CompletableFuture.completedFuture(false);
            }
        }
        // 最终指向的emitter对象
        SseEmitter finalEmitter = emitter;
        StringBuffer sb = new StringBuffer();
        // 开始对话，返回token流
        // 封装插入的信息对象
        AiMessagePair aiMessagePair = new AiMessagePair();
        aiMessagePair.setSseSessionId(sessionId);
        aiMessagePair.setSessionId(aiChatParamDTO.getChatSessionId());

        // 标志位，判断是否更新成功，防止重复插入
        String LLMId = "dart";
        if (aiChatParamDTO.getId() != null && aiChatParamDTO.getId() != 0) {
            LLMId = aiConfigMapper.selectModelIdByPrimaryKey(aiChatParamDTO.getId());
            if (LLMId == null){
                LLMId = "dart"; // 默认模型
                aiMessagePair.setModelUsed(0); // 0代表默认模型
            }else {
                // 有效模型
                aiMessagePair.setModelUsed(aiChatParamDTO.getId());
            }
        }
        AtomicBoolean updated = new AtomicBoolean(false);
        try {
            aiChatMessageService.chatMessageStream(LLMId, aiChatParamDTO.getQuestion())
                    .onNext(token -> {
                        if (updated.get()) {
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
     * 估算字符串的 token 数量
     * TODO 使用onComplete返回response的token来统计
     * @param text 输入字符串
     * @return token 数量
     */
    public static int estimateTokenCount(String text) {
        int chineseCount = 0;
        int englishCount = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (String.valueOf(ch).matches("[\u4e00-\u9fa5]")) {
                chineseCount++;
            } else if (String.valueOf(ch).matches("[a-zA-Z]")) {
                englishCount++;
            }
        }
        int englishToken = (int) Math.ceil(englishCount / 4.0);
        int otherCount = text.length() - chineseCount - englishCount;
        return chineseCount + englishToken + otherCount;
    }

    /**
     * 尝试插入消息的方法
     */
    private void tryUpdateMessage(AiMessagePair message, int status, String content, AtomicBoolean flag) {
        if (flag.compareAndSet(false, true)) {
            message.setStatus(status);
            message.setAiContent(content);
            message.setTokens(estimateTokenCount(content));
            message.setResponseTime(Date.from(Instant.now()));
            aiMessagePairMapper.updateBySseIdSelective(message);
        }
    }

}
```

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