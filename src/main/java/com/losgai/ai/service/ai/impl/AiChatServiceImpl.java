package com.losgai.ai.service.ai.impl;

import cn.hutool.core.util.StrUtil;
import com.losgai.ai.dto.AiChatParamDTO;
import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.entity.ai.AiMessagePair;
import com.losgai.ai.entity.ai.AiSession;
import com.losgai.ai.enums.AiMessageStatusEnum;
import com.losgai.ai.global.SseEmitterManager;
import com.losgai.ai.mapper.AiConfigMapper;
import com.losgai.ai.mapper.AiMessagePairMapper;
import com.losgai.ai.mapper.AiSessionMapper;
import com.losgai.ai.mq.sender.AiMessageSender;
import com.losgai.ai.service.ai.AiChatService;
import com.losgai.ai.util.ChatClientFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    private final AiMessagePairMapper aiMessagePairMapper;

    private final SseEmitterManager emitterManager;

    private final AiConfigMapper aiConfigMapper;

    private final AiSessionMapper aiSessionMapper;

    private final AiMessageSender aiMessageSender;

    private final ChatClientFactory chatClientFactory;

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${ai-chat-demo.sse.stream-concurrency-limit:50}")
    private int streamConcurrencyLimit;

    private Semaphore streamSemaphore;

    @PostConstruct
    private void initSemaphore() {
        this.streamSemaphore = new Semaphore(streamConcurrencyLimit);
    }

    /**
     * @param aiChatParamDTO 对话参数
     * @param sessionId      SSE会话ID
     * @return 是否处理成功
     * @apiNote AI对话请求，基于虚拟线程实现异步处理，SpringAI实现
     */
    @Override
    public CompletableFuture<Boolean> sendQuestionAsyncWithMemo(AiChatParamDTO aiChatParamDTO,
                                                                String sessionId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Thread.ofVirtual().start(() -> {
            try {
                if (emitterManager.isOverLoad()) {
                    future.complete(false);
                    return;
                }
                SseEmitter emitter = emitterManager.getEmitter(sessionId);
                emitterManager.notifyThreadCount();
                if (emitter == null) {
                    if (emitterManager.addEmitter(sessionId, new SseEmitter(600000L))) {
                        emitter = emitterManager.getEmitter(sessionId);
                    } else {
                        future.complete(false);
                        return;
                    }
                }

                AiConfig aiConfig = aiConfigMapper.selectByPrimaryKey(aiChatParamDTO.getModelId());
                if (aiConfig == null) {
                    future.complete(false);
                    return;
                }
                Long conversationId = aiChatParamDTO.getConversationId();
                if (conversationId == null) {
                    future.complete(false);
                    return;
                }

                // 限流：获取信号量
                try {
                    streamSemaphore.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    future.complete(false);
                    return;
                }

                // rawContent: 存库用原始文本; displaySb: SSE展示用转义文本
                StringBuilder rawContent = new StringBuilder();
                StringBuilder reasoningContent = new StringBuilder();
                AiMessagePair aiMessagePair = new AiMessagePair();
                aiMessagePair.setSseSessionId(sessionId);
                aiMessagePair.setSessionId(aiChatParamDTO.getConversationId());

                AtomicReference<ChatResponse> lastResponse = new AtomicReference<>();
                AtomicBoolean isInterrupted = new AtomicBoolean(false);
                // 防止 onError/onComplete/timeout 重复清理
                AtomicBoolean cleaned = new AtomicBoolean(false);
                AtomicReference<Disposable> disposableRef = new AtomicReference<>();
                AtomicBoolean semaphoreReleased = new AtomicBoolean(false);

                Runnable releaseSemaphore = () -> {
                    if (semaphoreReleased.compareAndSet(false, true)) {
                        streamSemaphore.release();
                    }
                };

                SseEmitter finalEmitter = emitter;
                finalEmitter.onCompletion(() -> emitterManager.removeEmitter(sessionId));
                // 超时时dispose响应式流并保存已生成内容
                finalEmitter.onTimeout(() -> {
                    log.warn("SSE emitter 超时, sessionId: {}", sessionId);
                    isInterrupted.set(true);
                    Disposable d = disposableRef.get();
                    if (d != null && !d.isDisposed()) {
                        d.dispose();
                    }
                    String finalContent = buildFinalContent(rawContent, reasoningContent);
                    int usageCount = extractUsage(lastResponse);
                    tryUpdateMessage(aiMessagePair, finalContent, true, usageCount);
                    cleanupEmitters(cleaned, sessionId);
                    releaseSemaphore.run();
                });

                // 在流式订阅前同步INSERT，消除前端INSERT与后端UPDATE的竞态
                aiMessagePair.setUserContent(aiChatParamDTO.getQuestion());
                aiMessagePair.setModelUsed(aiChatParamDTO.getModelId());
                aiMessagePair.setStatus(AiMessageStatusEnum.GENERATING.getCode());
                aiMessagePair.setCreateTime(Date.from(Instant.now()));
                aiMessagePairMapper.insertSelective(aiMessagePair);
                // 清除该会话的消息缓存
                redisTemplate.delete("aiMessagePairCache::" + aiMessagePair.getSessionId());
                // 更新会话最后消息时间
                AiSession sessionUpdate = new AiSession();
                sessionUpdate.setId(aiMessagePair.getSessionId());
                sessionUpdate.setLastMessageTime(aiMessagePair.getCreateTime());
                aiSessionMapper.updateByPrimaryKeySelective(sessionUpdate);

                // 提前完成future，释放Tomcat线程
                future.complete(true);

                // 分离streamChat调用，捕获同步异常并释放信号量
                Flux<ChatResponse> flux;
                try {
                    flux = chatClientFactory.streamChat(
                                    aiConfig,
                                    aiChatParamDTO.getUrlList(),
                                    aiChatParamDTO.getQuestion(),
                                    String.valueOf(conversationId),
                                    sessionId,
                                    finalEmitter);
                } catch (Exception e) {
                    log.error("streamChat同步阶段异常, sessionId: {}", sessionId, e);
                    try {
                        aiMessagePair.setStatus(AiMessageStatusEnum.STOPPED.getCode());
                        aiMessagePair.setAiContent("");
                        aiMessagePair.setResponseTime(Date.from(Instant.now()));
                        aiMessagePairMapper.updateBySseIdSelective(aiMessagePair);
                    } catch (Exception ex) {
                        log.error("streamChat异常后更新消息失败, sessionId: {}", sessionId, ex);
                    }
                    releaseSemaphore.run();
                    return;
                }

                disposableRef.set(flux.subscribe(
                        token -> {
                            if (token.getResult() != null) {
                                // 提取思考过程
                                Map<String, Object> metadata = token.getResult().getOutput().getMetadata();
                                String reasoning = metadata != null ? (String) metadata.get("reasoningContent") : null;
                                if (StrUtil.isNotBlank(reasoning)) {
                                    reasoningContent.append(reasoning);
                                    String escaped = reasoning.replace("\n", "<br>").replace(" ", "&nbsp;");
                                    try {
                                        if (emitterManager.getEmitter(sessionId) == null) {
                                            log.info("===>SSE已经被手动中断");
                                            isInterrupted.set(true);
                                            Disposable d = disposableRef.get();
                                            if (d != null) d.dispose();
                                        } else {
                                            emitterManager.touchActivity(sessionId);
                                            finalEmitter.send(SseEmitter.event().name("thinking").data(escaped));
                                        }
                                    } catch (IOException | IllegalStateException e) {
                                        log.debug("SSE发送思考内容失败, 客户端可能已断开, sessionId: {}", sessionId);
                                        if (cleaned.compareAndSet(false, true)) {
                                            isInterrupted.set(true);
                                            Disposable d = disposableRef.get();
                                            if (d != null) d.dispose();
                                            String finalContent = buildFinalContent(rawContent, reasoningContent);
                                            int usageCount = extractUsage(lastResponse);
                                            tryUpdateMessage(aiMessagePair, finalContent, true, usageCount);
                                            emitterManager.removeEmitter(sessionId);
                                            releaseSemaphore.run();
                                        }
                                        return;
                                    }
                                }

                                // 提取正式回复
                                String text = token.getResult().getOutput().getText();
                                if (StrUtil.isNotBlank(text)) {
                                    rawContent.append(text);
                                    text = text.replace("\n", "<br>");
                                    text = text.replace(" ", "&nbsp;");
                                    try {
                                        if (emitterManager.getEmitter(sessionId) == null) {
                                            log.info("===>SSE已经被手动中断，执行onComplete");
                                            isInterrupted.set(true);
                                            Disposable d = disposableRef.get();
                                            if (d != null) d.dispose();
                                        } else {
                                            emitterManager.touchActivity(sessionId);
                                            finalEmitter.send(SseEmitter.event().data(text));
                                        }
                                    } catch (IOException | IllegalStateException e) {
                                        log.debug("SSE发送失败, 客户端可能已断开, sessionId: {}", sessionId);
                                        if (cleaned.compareAndSet(false, true)) {
                                            isInterrupted.set(true);
                                            Disposable d = disposableRef.get();
                                            if (d != null) d.dispose();
                                            String finalContent = buildFinalContent(rawContent, reasoningContent);
                                            int usageCount = extractUsage(lastResponse);
                                            tryUpdateMessage(aiMessagePair, finalContent, true, usageCount);
                                            emitterManager.removeEmitter(sessionId);
                                            releaseSemaphore.run();
                                        }
                                        return;
                                    }
                                }
                            }
                            lastResponse.set(token);
                        },
                        e -> {
                            if (isClientDisconnect(e)) {
                                log.debug("客户端已断开连接, sessionId: {}", sessionId);
                            } else {
                                log.error("ai对话 流式输出报错, sessionId: {}", sessionId, e);
                            }
                            try {
                                int usageCount = extractUsage(lastResponse);
                                String finalContent = buildFinalContent(rawContent, reasoningContent);
                                tryUpdateMessage(aiMessagePair, finalContent, true, usageCount);
                            } catch (Exception ex) {
                                log.error("onError内更新消息失败, sessionId={}", sessionId, ex);
                            }
                            try {
                                finalEmitter.completeWithError(e);
                            } catch (Exception ignored) {}
                            cleanupEmitters(cleaned, sessionId);
                            releaseSemaphore.run();
                        },
                        () -> {
                            log.info("回答完毕！");
                            try {
                                int usageCount = extractUsage(lastResponse);
                                String finalContent = buildFinalContent(rawContent, reasoningContent);
                                log.info("最终拼接的数据:\n{}", finalContent);
                                log.info("token使用:{}", usageCount);
                                tryUpdateMessage(aiMessagePair, finalContent, isInterrupted.get(), usageCount);
                            } catch (Exception ex) {
                                log.error("onComplete内更新消息失败, sessionId={}", sessionId, ex);
                            }
                            finalEmitter.complete();
                            cleanupEmitters(cleaned, sessionId);
                            releaseSemaphore.run();
                            try {
                                AiMessagePair record = aiMessagePairMapper.selectBySseSessionId(sessionId);
                                if (record != null) {
                                    aiMessageSender.sendMessage("ai.exchange", "ai.message", record);
                                } else {
                                    log.warn("onComplete: selectBySseSessionId返回null, sessionId={}", sessionId);
                                }
                            } catch (Exception ex) {
                                log.error("onComplete内发送MQ消息失败, sessionId={}", sessionId, ex);
                            }
                        }));
            } catch (Exception e) {
                log.error("AI对话任务异常", e);
                future.complete(false);
            }
        });
        return future;
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
     * 从最后一个 ChatResponse 中提取 Token 使用量
     */
    private int extractUsage(AtomicReference<ChatResponse> lastResponse) {
        ChatResponse chatResponse = lastResponse.get();
        if (chatResponse != null && chatResponse.getMetadata() != null
                && chatResponse.getMetadata().getUsage() != null) {
            return chatResponse.getMetadata().getUsage().getTotalTokens();
        }
        log.warn("未获取到 Token 使用信息，可能模型未返回或配置未启用");
        return 0;
    }

    private boolean isClientDisconnect(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof org.springframework.web.context.request.async.AsyncRequestNotUsableException) {
                return true;
            }
            String msg = cause.getMessage();
            if (msg != null && msg.contains("中止了一个已建立的连接")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * 安全清理 emitter，防止 onError/onComplete 竞态重复移除
     */
    private void cleanupEmitters(AtomicBoolean cleaned, String sessionId) {
        if (cleaned.compareAndSet(false, true)) {
            emitterManager.removeEmitter(sessionId);
        }
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
        // 更新完成后清除缓存，确保下次查询拿到最新数据
        redisTemplate.delete("aiMessagePairCache::" + message.getSessionId());
        AiSession aiSession = new AiSession();
        aiSession.setId(message.getSessionId());
        aiSession.setLastMessageTime(message.getResponseTime());
        aiSessionMapper.updateByPrimaryKeySelective(aiSession);
    }

    /**
     * 拼接思考过程和正式回复，思考过程用 <thinking> 标签包裹
     */
    private String buildFinalContent(StringBuilder rawContent, StringBuilder reasoningContent) {
        String reasoning = reasoningContent.toString();
        if (StrUtil.isNotBlank(reasoning)) {
            return "<thinking>" + reasoning + "</thinking>" + rawContent;
        }
        return rawContent.toString();
    }

}
