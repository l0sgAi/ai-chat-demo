package com.losgai.ai;

import com.losgai.ai.tools.DateTimeTools;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.UrlResource;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@SpringBootTest
public class SpringAITests {

    @Autowired
    private AsyncMcpToolCallbackProvider toolCallbackProvider;

    @Test
    public void springAIStreamChatWithMemo() throws InterruptedException {

        // 计时器
        CountDownLatch countDownLatch = new CountDownLatch(1);

        // 为对话提供高级指令。例如，您可以使用系统消息指示生成器像某个角色一样行事，或以特定格式提供答案。
        Message sysMessage = // 添加系统消息
                new SystemMessage("你是一个负责任的、礼貌的AI助手。");

        // 用户输入的问题文本，它们代表问题、提示或您希望生成器响应的任何输入。
        Message userMessage =
                new UserMessage("帮我搜索CS2 2025 IEM成都的比赛结果");

        // 提供有关对话中先前交流的背景信息
        Message assistantMessage =
                new AssistantMessage("我们正在交流关于英雄联盟S赛的相关问题");

        OpenAiApi openAiApi = OpenAiApi.builder()
                // 填入自己的API KEY
                .apiKey("sk-*****")
                // 填入自己的API域名，如果是百炼，即为https://dashscope.aliyuncs.com/compatible-mode
                // 注意：这里与langchain4j的配置不同，不需要在后面加/v1
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                .build();

        // 模型选项
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                // 模型生成的最大 tokens 数
                .maxTokens(2048)
                // 模型生成的 tokens 的概率质量范围，取值范围 0.0-1.0 越大的概率质量范围越大
                .topP(0.9)
                // 模型生成的 tokens 的随机度，取值范围 0.0-1.0 越大的随机度越大
                .temperature(0.9)
                // 模型名称
                .model("qwen-turbo-latest")
                // 打开流式对话token计数配置，默认为false
                .streamUsage(true)
                .toolCallbacks(toolCallbackProvider.getToolCallbacks())
                .build();

        // 工具调用管理器 暂时为空
        ToolCallingManager toolCallingManager = ToolCallingManager
                .builder()
                .build();

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
//                .content("你是谁？能做什么？之前我们在干什么？")
                // 添加系统、用户、背景信息
                .messages(sysMessage, userMessage, assistantMessage)
                .build();

        // 反应式对话流
        Flux<ChatResponse> responseFlux = model.stream(prompt);
        // 用于跟踪最后一个 ChatResponse
        AtomicReference<ChatResponse> lastResponse = new AtomicReference<>();

        // 订阅 Flux 实现流式输出（控制台输出或 SSE 推送）
        responseFlux.subscribe(
                token -> {
                    // 获取当前输出内容片段
                    if(token.getResult()!=null){
                        log.info("输出内容:{}", token.getResult().getOutput().getText());
                    }
                    // 更新最后一个响应
                    lastResponse.set(token);
                },
                // 反应式流在报错时会直接中断
                error -> {
                    log.error("出错：", error);
                    // 错误，停止倒计时
                    countDownLatch.countDown();
                }, // 错误处理
                () -> {// 流结束
                    log.info("\n回答完毕！");
                    // 从最后一个响应中获取 Token 使用信息
                    ChatResponse chatResponse = lastResponse.get();
                    if (chatResponse != null) {
                        Usage usage = chatResponse.getMetadata().getUsage();
                        log.info("===== Token 使用统计 =====");
                        log.info("输入 Token 数（Prompt Tokens）: {}", usage.getPromptTokens());
                        log.info("输出 Token 数（Completion Tokens）: {}", usage.getCompletionTokens());
                        log.info("总 Token 数（Total Tokens）: {}", usage.getTotalTokens());
                    } else {
                        log.warn("未获取到 Token 使用信息，可能模型未返回或配置未启用");
                    }
                    countDownLatch.countDown();
                });

        // 阻塞主线程最多60s 等待结果
        countDownLatch.await(60, TimeUnit.SECONDS);
    }

    @Test
    public void springAIStreamChatWithMemoClient() throws InterruptedException {

        // 计时器
        CountDownLatch countDownLatch = new CountDownLatch(1);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey("sk-***")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .completionsPath("/chat/completions")
                .build();

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .maxTokens(2048)
                .temperature(0.85)
                .topP(0.9)
                .model("qwen-turbo-latest")
                .streamUsage(true)
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatOptions)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel)
                // 默认工具调用
                .defaultTools(new DateTimeTools())
                // 默认系统提示词
                .defaultSystem("你是一个由losgai开发的、友善的AI助手，请保持你的语气平和、耐心、礼貌。")
                .build();

        // 反应式对话流
        Flux<ChatResponse> responseFlux = chatClient.prompt()
                .user("帮我搜索英雄联盟S15世界赛结果")
                .toolCallbacks(toolCallbackProvider.getToolCallbacks())
                .stream()
                .chatResponse();

        // 用于跟踪最后一个 ChatResponse
        AtomicReference<ChatResponse> lastResponse = new AtomicReference<>();

        // 订阅 Flux 实现流式输出（控制台输出或 SSE 推送）
        StringBuffer res = new StringBuffer();
        responseFlux.subscribe(
                token -> {
                    // 获取当前输出内容片段
                    if(token.getResult()!=null){
                        log.info("输出内容:{}", token.getResult().getOutput().getText());
                        res.append(token.getResult().getOutput().getText());
                    }
                    // 更新最后一个响应
                    lastResponse.set(token);
                },
                // 反应式流在报错时会直接中断
                error -> {
                    log.error("出错：", error);
                    // 错误，停止倒计时
                    countDownLatch.countDown();
                }, // 错误处理
                () -> {// 流结束
                    log.info("\n回答完毕！");
                    // 从最后一个响应中获取 Token 使用信息
                    ChatResponse chatResponse = lastResponse.get();
                    if (chatResponse != null) {
                        Usage usage = chatResponse.getMetadata().getUsage();
                        log.info("===== Token 使用统计 =====");
                        log.info("输入 Token 数（Prompt Tokens）: {}", usage.getPromptTokens());
                        log.info("输出 Token 数（Completion Tokens）: {}", usage.getCompletionTokens());
                        log.info("总 Token 数（Total Tokens）: {}", usage.getTotalTokens());
                        log.info("回复汇总：\n{}", res);
                    } else {
                        log.warn("未获取到 Token 使用信息，可能模型未返回或配置未启用");
                    }
                    countDownLatch.countDown();
                });

        // 阻塞主线程最多60s 等待结果
        countDownLatch.await(60, TimeUnit.SECONDS);
    }

    @Test
    public void springAIStreamChat() throws InterruptedException {

        // 计时器
        CountDownLatch countDownLatch = new CountDownLatch(1);

        // 为对话提供高级指令。例如，您可以使用系统消息指示生成器像某个角色一样行事，或以特定格式提供答案。
        Message sysMessage = // 添加系统消息
                new SystemMessage("你是一个负责任的、礼貌的AI助手。");

        // 用户输入的问题文本，它们代表问题、提示或您希望生成器响应的任何输入。
        Message userMessage =
                new UserMessage("你是Claude-4.0吗？能做什么？之前我们在干什么？");

        // 提供有关对话中先前交流的背景信息
        Message assistantMessage =
                new AssistantMessage("我们正在交流关于SpringAI技术的相关问题");

        OpenAiApi openAiApi = OpenAiApi.builder()
                // 填入自己的API KEY
                .apiKey("sk-...")
                // 填入自己的API域名，如果是百炼，即为https://dashscope.aliyuncs.com/compatible-mode
                // 注意：这里与langchain4j的配置不同，不需要在后面加/v1
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                .build();

        // 模型选项
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                // 模型生成的最大 tokens 数
                .maxTokens(2048)
                // 模型生成的 tokens 的概率质量范围，取值范围 0.0-1.0 越大的概率质量范围越大
                .topP(0.9)
                // 模型生成的 tokens 的随机度，取值范围 0.0-1.0 越大的随机度越大
                .temperature(0.9)
                // 模型名称
                .model("qwen-turbo-latest")
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
//                .content("你是谁？能做什么？之前我们在干什么？")
                // 添加系统、用户、背景信息
                .messages(sysMessage, userMessage, assistantMessage)
                .build();

        // 反应式对话流
        Flux<ChatResponse> responseFlux = model.stream(prompt);
        // 用于跟踪最后一个 ChatResponse
        AtomicReference<ChatResponse> lastResponse = new AtomicReference<>();

        // 订阅 Flux 实现流式输出（控制台输出或 SSE 推送）
        responseFlux.subscribe(
                token -> {
                    // 获取当前输出内容片段
                    if(token.getResult()!=null){
                        log.info("输出内容:{}", token.getResult().getOutput().getText());
                    }
                    // 更新最后一个响应
                    lastResponse.set(token);
                },
                // 反应式流在报错时会直接中断
                error -> {
                    log.error("出错：", error);
                    // 错误，停止倒计时
                    countDownLatch.countDown();
                }, // 错误处理
                () -> {// 流结束
                    log.info("\n回答完毕！");
                    // 从最后一个响应中获取 Token 使用信息
                    ChatResponse chatResponse = lastResponse.get();
                    if (chatResponse != null) {
                        Usage usage = chatResponse.getMetadata().getUsage();
                        log.info("===== Token 使用统计 =====");
                        log.info("输入 Token 数（Prompt Tokens）: {}", usage.getPromptTokens());
                        log.info("输出 Token 数（Completion Tokens）: {}", usage.getCompletionTokens());
                        log.info("总 Token 数（Total Tokens）: {}", usage.getTotalTokens());
                    } else {
                        log.warn("未获取到 Token 使用信息，可能模型未返回或配置未启用");
                    }
                    countDownLatch.countDown();
                });

        // 阻塞主线程最多60s 等待结果
        countDownLatch.await(60, TimeUnit.SECONDS);
    }

    /**
     * 测试多模态模型流式输出
     * */
    @Test
    public void openAI() throws InterruptedException, MalformedURLException {
        // 计时器
        CountDownLatch countDownLatch = new CountDownLatch(1);
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey("sk-***")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                .build();
        OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
                .model("qwen-omni-turbo-latest")
                .temperature(0.9)
                .topP(0.9)
                .streamUsage(true)
                .maxTokens(2048)
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(openAiChatOptions)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel)
//                .defaultAdvisors(MessageChatMemoryAdvisor.builder(mybatisChatMemory).build())
                .build();

        Media media1 = Media.builder()
                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .data(new UrlResource("http://192.168.200.132:9001/ai-chat-multimodel-bucket/20250726/99302c548fe3460eb574d3e0ccf012b3_屏幕截图 2025-07-23 112915.png"))
                .build();

        Media media2 = Media.builder()
                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .data(new UrlResource("http://192.168.200.132:9001/ai-chat-multimodel-bucket/20250726/5c05a35b73574c1686025dbcde80b880_屏幕截图 2025-07-23 112915.png"))
                .build();

        Media[] mediaArray = {media1,media2};

        Flux<ChatResponse> content = chatClient.prompt()
                .user(u -> {
                    u.text("这2张图片是一样的吗？")
                            .media(mediaArray);
                })
                .stream()
                .chatResponse();

        content.subscribe(token -> log.info("输出内容:{}", token.getResult().getOutput().getText()));
        // 阻塞主线程最多60s 等待结果
        countDownLatch.await(60, TimeUnit.SECONDS);
    }

}
