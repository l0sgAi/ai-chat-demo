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
import com.losgai.ai.util.ModelBuilderSpringAiWithMemo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

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

    private final AiConfigMapper aiConfigMapper;

    private final ModelBuilderSpringAiWithMemo modelBuilderSpringAiWithMemo;

    private final AiSessionMapper aiSessionMapper;

    private final AiMessageSender aiMessageSender;

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
            if (conversationId == null) {
                return false;
            }
            Flux<ChatResponse> chatResponseFlux = modelBuilderSpringAiWithMemo.buildModelStreamWithMemo(aiConfig,
                    aiChatParamDTO.getUrlList(),
                    "你是一个友善的AI助手",
                    aiChatParamDTO.getQuestion(),
                    String.valueOf(conversationId));
            // 用于跟踪最后一个 ChatResponse
            AtomicReference<ChatResponse> lastResponse = new AtomicReference<>();
            // 一个中断信号
            Sinks.One<Boolean> interruptSignal = Sinks.one();
            // 中断标志位
            AtomicBoolean isInterrupted = new AtomicBoolean(false);
            chatResponseFlux
                    .takeUntilOther(interruptSignal.asMono())
                    .subscribe(
                            token -> {
                                // 获取当前输出内容片段
                                String text = "";
                                String reasoningText = "";
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
                                        // 中断条件
                                        if (emitterManager.getEmitter(sessionId) == null) {
                                            log.info("===>SSE已经被手动中断，执行onComplete");
                                            isInterrupted.set(true);
                                            interruptSignal.tryEmitValue(true);
                                        } else {
                                            finalEmitter.send(SseEmitter.event().data(text));
                                        }
                                    }
                                } catch (IOException e) {
                                    log.error("===>SSE发送异常：{}", e.getMessage());
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
                                        isInterrupted.get(),
                                        usageCount);
                                // 新增部分：消息队列发送
                                // exchange 是交换机，决定消息往哪里发。
                                // routingKey 是路由键，告诉交换机这条消息具体发给哪个队列。
                                aiMessageSender.sendMessage("ai.exchange", "ai.message", aiMessagePairMapper.selectBySseSessionId(sessionId));
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
        AiSession aiSession = new AiSession();
        aiSession.setId(message.getSessionId());
        aiSession.setLastMessageTime(message.getResponseTime());
        aiSessionMapper.updateByPrimaryKeySelective(aiSession);
    }

}
