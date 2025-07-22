package com.losgai.ai.controller.ai;

import com.losgai.ai.common.Result;
import com.losgai.ai.dto.AiChatParamDTO;
import com.losgai.ai.service.ai.AiChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @PostMapping("/send")
    @Tag(name = "ai会话请求",description = "通过参数获取AI对话请求，返回唯一会话id")
    public Result<String> sendQuestion(@RequestBody AiChatParamDTO aiChatParamDTO) {
        String sessionId = UUID.randomUUID() + "_" + currentTimeMillis();
//        CompletableFuture<Boolean> future = aiChatService.sendQuestionAsync(aiChatParamDTO, sessionId);
        CompletableFuture<Boolean> future = aiChatService.sendQuestionAsyncWithMemo(aiChatParamDTO, sessionId);
        // 立即获取结果（因为拒绝时会同步返回）
        boolean accepted = future.join();
        if (!accepted) {
            return Result.error("系统繁忙，请稍后再试");
        }
        return Result.success(sessionId);
    }

    @GetMapping("/stream/{sessionId}")
    @Tag(name = "SSE对话流获取",description = "用sessionId建立连接，获取数据流")
    public SseEmitter stream(@PathVariable String sessionId) {
        return aiChatService.getEmitter(sessionId);
    }

}