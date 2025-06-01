package com.losgai.ai.controller;

import com.losgai.ai.common.Result;
import com.losgai.ai.entity.AiMessagePair;
import com.losgai.ai.service.AiMessagePairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/email/ai/message/pair")
public class AiMessagePairController {
    
    private final AiMessagePairService aiMessagePairService;

    @PostMapping("/add")
    public Result<String> add(@RequestBody AiMessagePair aiMessagePair) {
        aiMessagePairService.addMessage(aiMessagePair);
        return Result.success("新增消息成功！");
    }
    
    @GetMapping("select/{sessionId}")
    public Result<List<AiMessagePair>> getAiChatMessage(@PathVariable("sessionId") Long sessionId) {
        return Result.success(aiMessagePairService.selectBySessionId(sessionId));
    }
    
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        aiMessagePairService.deleteBySessionId(id);
        return Result.success("删除成功！");
    }
    
}
