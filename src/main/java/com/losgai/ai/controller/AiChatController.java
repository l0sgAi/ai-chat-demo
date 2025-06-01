package com.losgai.ai.controller;

import com.losgai.ai.common.Result;
import com.losgai.ai.dto.AiChatParamDTO;
import com.losgai.ai.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.lang.System.currentTimeMillis;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
@Slf4j
public class AiChatController {

    private final AiChatService aiChatService;

    // 1. HTTP POST接口，返回sessionId
    @PostMapping("/send")
    public Result<String> sendQuestion(@RequestBody AiChatParamDTO aiChatParamDTO) {
        String sessionId = UUID.randomUUID() + "_" + currentTimeMillis();
        CompletableFuture<Boolean> future = aiChatService.handleQuestionAsyncByVirtualThread(aiChatParamDTO, sessionId);
        // 立即获取结果（因为拒绝时会同步返回）
        boolean accepted = future.join();
        if (!accepted) {
            return Result.error("系统繁忙，请稍后再试");
        }
        return Result.success(sessionId);
    }

    // 2. SSE接口，客户端用sessionId建立连接
    @GetMapping("/stream/{sessionId}")
    public SseEmitter stream(@PathVariable String sessionId) {
        return aiChatService.getEmitter(sessionId);
    }
}