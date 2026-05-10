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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
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
                    if (emitterManager.addEmitter(sessionId, new SseEmitter(0L))) {
                        emitter = emitterManager.getEmitter(sessionId);
                    } else {
                        future.complete(false);
                        return;
                    }
                }
                SseEmitter finalEmitter = emitter;
                // rawContent: 存库用原始文本; displaySb: SSE展示用转义文本
                StringBuilder rawContent = new StringBuilder();
                AiMessagePair aiMessagePair = new AiMessagePair();
                aiMessagePair.setSseSessionId(sessionId);
                aiMessagePair.setSessionId(aiChatParamDTO.getChatSessionId());

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

                AtomicReference<ChatResponse> lastResponse = new AtomicReference<>();
                AtomicBoolean isInterrupted = new AtomicBoolean(false);
                // 防止 onError/onComplete 重复清理
                AtomicBoolean cleaned = new AtomicBoolean(false);
                AtomicReference<Disposable> disposableRef = new AtomicReference<>();

                disposableRef.set(chatClientFactory.streamChat(
                                aiConfig,
                                aiChatParamDTO.getUrlList(),
                                aiChatParamDTO.getQuestion(),
                                String.valueOf(conversationId))
                        .subscribe(
                                token -> {
                                    if (token.getResult() != null) {
                                        String text = token.getResult().getOutput().getText();
                                        if (StrUtil.isNotBlank(text)) {
                                            rawContent.append(text);
                                            text = text.replace("\n", "<br>");
                                            text = text.replace(" ", "&nbsp;");
                                        }
                                        try {
                                            if (StrUtil.isNotBlank(text)) {
                                                if (emitterManager.getEmitter(sessionId) == null) {
                                                    log.info("===>SSE已经被手动中断，执行onComplete");
                                                    isInterrupted.set(true);
                                                    Disposable d = disposableRef.get();
                                                    if (d != null) d.dispose();
                                                } else {
                                                    finalEmitter.send(SseEmitter.event().data(text));
                                                }
                                            }
                                        } catch (IOException e) {
                                            log.error("===>SSE发送异常", e);
                                            throw new RuntimeException(e);
                                        }
                                    }
                                    lastResponse.set(token);
                                },
                                e -> {
                                    log.error("ai对话 流式输出报错", e);
                                    int usageCount = extractUsage(lastResponse);
                                    tryUpdateMessage(aiMessagePair, rawContent.toString(), true, usageCount);
                                    finalEmitter.completeWithError(e);
                                    cleanupEmitters(cleaned, sessionId, conversationId);
                                },
                                () -> {
                                    log.info("回答完毕！");
                                    int usageCount = extractUsage(lastResponse);
                                    finalEmitter.complete();
                                    cleanupEmitters(cleaned, sessionId, conversationId);
                                    log.info("最终拼接的数据:\n{}", rawContent);
                                    log.info("token使用:{}", usageCount);
                                    tryUpdateMessage(aiMessagePair, rawContent.toString(), isInterrupted.get(), usageCount);
                                    aiMessageSender.sendMessage("ai.exchange", "ai.message", aiMessagePairMapper.selectBySseSessionId(sessionId));
                                }));
                future.complete(true);
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

    /**
     * 安全清理 emitter，防止 onError/onComplete 竞态重复移除
     */
    private void cleanupEmitters(AtomicBoolean cleaned, String sessionId, Long conversationId) {
        if (cleaned.compareAndSet(false, true)) {
            emitterManager.removeEmitter(sessionId);
            emitterManager.removeEmitter(String.valueOf(conversationId));
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
        AiSession aiSession = new AiSession();
        aiSession.setId(message.getSessionId());
        aiSession.setLastMessageTime(message.getResponseTime());
        aiSessionMapper.updateByPrimaryKeySelective(aiSession);
    }

}
