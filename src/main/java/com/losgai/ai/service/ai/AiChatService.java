package com.losgai.ai.service.ai;

import com.losgai.ai.dto.AiChatParamDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

public interface AiChatService {

    /**
     * 获取会话
     */
    @Deprecated
    CompletableFuture<Boolean> handleQuestionAsync(AiChatParamDTO aiChatParamDTO, String sessionId);

    
    /**
     * 获取会话 虚拟线程
     */
    @Deprecated
    CompletableFuture<Boolean> handleQuestionAsyncByVirtualThread(AiChatParamDTO aiChatParamDTO, String sessionId);

    /**
     * 反应流式对话，带记忆
     * */
    CompletableFuture<Boolean> sendQuestionAsyncWithMemo(AiChatParamDTO aiChatParamDTO, String sessionId);

    /**
     * 反应流处理流式对话
     * */
    CompletableFuture<Boolean> sendQuestionAsync(AiChatParamDTO aiChatParamDTO, String sessionId);
    

    /** 获取流式返回结果*/
    SseEmitter getEmitter(String sessionId);
}
