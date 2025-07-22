package com.losgai.ai.service.ai.impl;

import cn.hutool.core.util.StrUtil;
import com.losgai.ai.dto.AiChatParamDTO;
import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.entity.ai.AiMessagePair;
import com.losgai.ai.enums.AiMessageStatusEnum;
import com.losgai.ai.global.SseEmitterManager;
import com.losgai.ai.mapper.AiConfigMapper;
import com.losgai.ai.mapper.AiMessagePairMapper;
import com.losgai.ai.memory.MybatisChatMemory;
import com.losgai.ai.service.ai.AiChatService;
import com.losgai.ai.service.ai.AiChatMessageService;
import com.losgai.ai.util.ModelBuilderSpringAiWithMemo;
import com.losgai.ai.util.OpenAiModelBuilderSpringAi;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    private final AiMessagePairMapper aiMessagePairMapper;

    private final SseEmitterManager emitterManager;

    private final AiChatMessageService aiChatMessageService;

    private final AiConfigMapper aiConfigMapper;

    private final MybatisChatMemory mybatisChatMemory;

    private final ModelBuilderSpringAiWithMemo modelBuilderSpringAiWithMemo;

    /**
     * @param aiChatParamDTO 对话参数
     * @param sessionId      SSE会话ID
     * @return 是否处理成功
     * @apiNote AI对话请求，基于虚拟线程实现异步处理，SpringAI实现
     */
    @Override
    public CompletableFuture<Boolean> sendQuestionAsyncWithMemo(AiChatParamDTO aiChatParamDTO, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            if (emitterManager.isOverLoad())
                return false;
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
                    return false;
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

            // 从数据库获取配置
            AiConfig aiConfig = aiConfigMapper.selectByPrimaryKey(aiChatParamDTO.getModelId());
            // 获取conversationId
            Long conversationId = aiChatParamDTO.getConversationId();
            if(conversationId == null){
                return false;
            }
            Flux<ChatResponse> chatResponseFlux = modelBuilderSpringAiWithMemo.buildModelStreamWithMemo(aiConfig,
                    "你是一个礼貌的AI助手",
                    aiChatParamDTO.getQuestion(),
                    "",
                    String.valueOf(conversationId));
            // 用于跟踪最后一个 ChatResponse
            AtomicReference<ChatResponse> lastResponse = new AtomicReference<>();
            chatResponseFlux.subscribe(
                    token -> {
                        // 获取当前输出内容片段
                        String text = "";
                        if (token.getResult() != null) {
                            text = token.getResult().getOutput().getText();
                        }
                        if (StrUtil.isNotBlank(text)) {
                            sb.append(text);
                            // log.info("当前段数据:{}", text);
                            // 换行符转义：token换行符转换成<br>
                            text = text.replace("\n", "<br>");
                            // 换行符转义：如果token以换行符为结尾，转换成<br>
                            text = text.replace(" ", "&nbsp;");
                        }
                        // 发送返回的数据
                        try {
                            if (StrUtil.isNotBlank(text)) {
                                finalEmitter.send(SseEmitter.event().data(text));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        // 更新最后一个响应
                        lastResponse.set(token);
                    },
                    // 反应式流在报错时会直接中断
                    e -> {
                        log.error("ai对话 流式输出报错:{}", e.getMessage());
                        int usageCount = 0;
                        ChatResponse chatResponse = lastResponse.get();
                        if (chatResponse != null) {
                            Usage usage = chatResponse.getMetadata().getUsage();
                            usageCount = usage.getTotalTokens();
                        } else {
                            log.warn("未获取到 Token 使用信息，可能模型未返回或配置未启用");
                        }
                        // 更新中断的状态
                        tryUpdateMessage(aiMessagePair,
                                sb.toString(),
                                true,
                                usageCount);
                        finalEmitter.completeWithError(e);
                        emitterManager.removeEmitter(sessionId); // 出错时也移除
                    }, // 错误处理
                    () -> {// 流结束
                        log.info("\n回答完毕！");
                        // 从最后一个响应中获取 Token 使用信息
                        ChatResponse chatResponse = lastResponse.get();
                        int usageCount = 0;
                        if (chatResponse != null) {
                            Usage usage = chatResponse.getMetadata().getUsage();
                            usageCount = usage.getTotalTokens();
                        } else {
                            log.warn("未获取到 Token 使用信息，可能模型未返回或配置未启用");
                        }
                        finalEmitter.complete();
                        emitterManager.removeEmitter(sessionId); // 只在流结束后移除
                        log.info("最终拼接的数据:\n{}", sb);
                        log.info("token使用:{}", usageCount);
                        // 更新正常完成的状态
                        tryUpdateMessage(aiMessagePair,
                                sb.toString(),
                                false,
                                usageCount);
                    });
            return true;
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * 异步处理AI对话请求，基于SSE实现流式输出。
     * <br>
     * 主要流程：
     * 1. 检查服务是否过载，获取或创建对应sessionId的SseEmitter。
     * 2. 发送队列人数通知，初始化消息对象。
     * 3. 调用AI流式接口，逐步推送token到前端。
     * 4. 根据对话完成或异常，更新数据库消息状态。
     * 5. 释放SseEmitter资源，保证系统健壮性。
     *
     * @param aiChatParamDTO 对话参数
     * @param sessionId      SSE会话ID
     * @return 是否处理成功
     */
    @Override
    @Async("aiWorkerExecutor")
    public CompletableFuture<Boolean> handleQuestionAsync(AiChatParamDTO aiChatParamDTO, String sessionId) {
        if (emitterManager.isOverLoad())
            return CompletableFuture.completedFuture(false);
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
        AiConfig aiConfig = aiConfigMapper.selectByPrimaryKey(aiChatParamDTO.getModelId());
        AtomicBoolean isErrored = new AtomicBoolean(false); // 是否中断的标志位
        try {
            aiChatMessageService.chatMessageStream(aiConfig, aiChatParamDTO.getQuestion())
                    .onNext(token -> {
                        // 注：这里只是不接受token，后台其实还在输出token流，0.35.0版本暂不支持流式对话中断
                        if (isErrored.get()) {
                            return; // 已取消，不再处理
                        }
                        try {
                            // 换行符转义：如果token以换行符为结尾，转换成<br>
                            sb.append(token);
                            token = token.replace("\n", "<br>");
                            token = token.replace(" ", "&nbsp;");
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
                        log.info("模型token使用:{}", response.tokenUsage());
                        tryUpdateMessage(aiMessagePair,
                                sb.toString(),
                                isErrored.get(),
                                response.tokenUsage().totalTokenCount());
                    })
                    .onError(e -> {
                        log.warn("ai对话 流式输出报错:{}", e.getMessage());
                        isErrored.set(true); // 标记为已取消
                        finalEmitter.completeWithError(e);
                        emitterManager.removeEmitter(sessionId); // 出错时也移除
                    })
                    .start();
        } catch (Exception e) {
            log.error("处理ai对话报错:{}", e.getMessage());
            finalEmitter.completeWithError(e);
            emitterManager.removeEmitter(sessionId); // 捕获到异常时移除
        }
        return CompletableFuture.completedFuture(true);
    }

    /**
     * AI对话请求，基于虚拟线程实现异步处理。
     *
     * @param aiChatParamDTO 对话参数
     * @param sessionId      SSE会话ID
     * @return 是否处理成功
     */
    @Override
    public CompletableFuture<Boolean> handleQuestionAsyncByVirtualThread(AiChatParamDTO aiChatParamDTO,
                                                                         String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            if (emitterManager.isOverLoad())
                return false;
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
                    return false;
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
            AiConfig aiConfig = aiConfigMapper.selectByPrimaryKey(aiChatParamDTO.getModelId());
            AtomicBoolean isErrored = new AtomicBoolean(false);
            try {
                aiChatMessageService.chatMessageStream(aiConfig, aiChatParamDTO.getQuestion())
                        .onNext(token -> {
                            if (isErrored.get()) {
                                return; // 已取消，不再处理
                            }
                            try {
                                // 换行符转义：如果token以换行符为结尾，转换成<br>
                                sb.append(token);
                                token = token.replace("\n", "<br>");
                                token = token.replace(" ", "&nbsp;");
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
                            log.info("模型token使用:{}", response.tokenUsage());
                            tryUpdateMessage(aiMessagePair,
                                    sb.toString(),
                                    isErrored.get(),
                                    response.tokenUsage().totalTokenCount());
                        })
                        .onError(e -> {
                            log.warn("ai对话 流式输出报错:{}", e.getMessage());
                            isErrored.set(true); // 标记为已取消
                            finalEmitter.completeWithError(e);
                            emitterManager.removeEmitter(sessionId); // 出错时也移除
                        })
                        .start();
            } catch (Exception e) {
                log.error("处理ai对话报错:{}", e.getMessage());
                finalEmitter.completeWithError(e);
                emitterManager.removeEmitter(sessionId); // 捕获到异常时移除
            }
            return true;
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * @param aiChatParamDTO 对话参数
     * @param sessionId      SSE会话ID
     * @return 是否处理成功
     * @apiNote AI对话请求，基于虚拟线程实现异步处理，SpringAI实现
     */
    @Override
    public CompletableFuture<Boolean> sendQuestionAsync(AiChatParamDTO aiChatParamDTO, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            if (emitterManager.isOverLoad())
                return false;
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
                    return false;
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
            AiConfig aiConfig = aiConfigMapper.selectByPrimaryKey(aiChatParamDTO.getModelId());
            Flux<ChatResponse> chatResponseFlux = OpenAiModelBuilderSpringAi.buildModelStream(aiConfig,
                    "你是一个礼貌的AI助手",
                    aiChatParamDTO.getQuestion(),
                    "");
            // 用于跟踪最后一个 ChatResponse
            AtomicReference<ChatResponse> lastResponse = new AtomicReference<>();
            chatResponseFlux.subscribe(
                    token -> {
                        // 获取当前输出内容片段
                        String text = "";
                        if (token.getResult() != null) {
                            text = token.getResult().getOutput().getText();
                        }
                        if (StrUtil.isNotBlank(text)) {
                            sb.append(text);
                            // log.info("当前段数据:{}", text);
                            // 换行符转义：token换行符转换成<br>
                            text = text.replace("\n", "<br>");
                            // 换行符转义：如果token以换行符为结尾，转换成<br>
                            text = text.replace(" ", "&nbsp;");
                        }
                        // 发送返回的数据
                        try {
                            if (StrUtil.isNotBlank(text)) {
                                finalEmitter.send(SseEmitter.event().data(text));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        // 更新最后一个响应
                        lastResponse.set(token);
                    },
                    // 反应式流在报错时会直接中断
                    e -> {
                        log.error("ai对话 流式输出报错:{}", e.getMessage());
                        int usageCount = 0;
                        ChatResponse chatResponse = lastResponse.get();
                        if (chatResponse != null) {
                            Usage usage = chatResponse.getMetadata().getUsage();
                            usageCount = usage.getTotalTokens();
                        } else {
                            log.warn("未获取到 Token 使用信息，可能模型未返回或配置未启用");
                        }
                        // 更新中断的状态
                        tryUpdateMessage(aiMessagePair,
                                sb.toString(),
                                true,
                                usageCount);
                        finalEmitter.completeWithError(e);
                        emitterManager.removeEmitter(sessionId); // 出错时也移除
                    }, // 错误处理
                    () -> {// 流结束
                        log.info("\n回答完毕！");
                        // 从最后一个响应中获取 Token 使用信息
                        ChatResponse chatResponse = lastResponse.get();
                        int usageCount = 0;
                        if (chatResponse != null) {
                            Usage usage = chatResponse.getMetadata().getUsage();
                            usageCount = usage.getTotalTokens();
                        } else {
                            log.warn("未获取到 Token 使用信息，可能模型未返回或配置未启用");
                        }
                        finalEmitter.complete();
                        emitterManager.removeEmitter(sessionId); // 只在流结束后移除
                        log.info("最终拼接的数据:{}", sb);
                        log.info("token使用:{}", usageCount);
                        // 更新正常完成的状态
                        tryUpdateMessage(aiMessagePair,
                                sb.toString(),
                                false,
                                usageCount);
                    });
            return true;
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * ai回答推流sse
     */
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
                                  String content,
                                  boolean isInterrupted,
                                  Integer tokenUsed) {
        int status = isInterrupted ? AiMessageStatusEnum.STOPPED.getCode() : AiMessageStatusEnum.FINISHED.getCode();
        message.setStatus(status);
        message.setAiContent(content);
        message.setTokens(tokenUsed);
        message.setResponseTime(Date.from(Instant.now()));
        aiMessagePairMapper.updateBySseIdSelective(message);
    }

}
