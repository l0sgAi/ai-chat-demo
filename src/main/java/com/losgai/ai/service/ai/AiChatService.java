package com.losgai.ai.service.ai;

import com.losgai.ai.dto.AiChatParamDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

public interface AiChatService {
    /**
     * 反应流式对话，带记忆
     * */
    CompletableFuture<Boolean> sendQuestionAsyncWithMemo(AiChatParamDTO aiChatParamDTO, String sessionId);

    /** 获取流式返回结果*/
    SseEmitter getEmitter(String sessionId);
}
