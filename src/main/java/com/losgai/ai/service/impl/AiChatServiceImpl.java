package com.losgai.ai.service.impl;

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
