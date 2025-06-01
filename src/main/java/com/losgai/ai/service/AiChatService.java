package com.losgai.ai.service;

import com.losgai.ai.dto.AiChatParamDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

public interface AiChatService {

    /**
     * 获取会话
     */
    CompletableFuture<Boolean> handleQuestionAsync(AiChatParamDTO aiChatParamDTO, String sessionId);

    /**
     * 获取会话 虚拟线程
     */
    CompletableFuture<Boolean> handleQuestionAsyncByVirtualThread(AiChatParamDTO aiChatParamDTO, String sessionId);

    /** 获取流式返回结果*/
    SseEmitter getEmitter(String sessionId);
}
