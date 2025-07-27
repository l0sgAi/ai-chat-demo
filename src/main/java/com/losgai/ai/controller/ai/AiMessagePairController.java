package com.losgai.ai.controller.ai;

import com.losgai.ai.common.sys.Result;
import com.losgai.ai.entity.ai.AiMessagePair;
import com.losgai.ai.global.EsConstants;
import com.losgai.ai.service.ai.AiMessagePairService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/message/pair")
public class AiMessagePairController {
    
    private final AiMessagePairService aiMessagePairService;

    @PostMapping("/add")
    @Tag(name = "新增消息",description = "新增问答对数据")
    public Result<String> add(@RequestBody AiMessagePair aiMessagePair) {
        aiMessagePairService.addMessage(aiMessagePair);
        return Result.success("新增消息成功！");
    }
    
    @GetMapping("/select/{sessionId}")
    @Tag(name = "查询会话消息",description = "根据会话id，查询会话消息")
    public Result<List<AiMessagePair>> getAiChatMessage(@PathVariable("sessionId") Long sessionId) {
        List<AiMessagePair> aiMessagePairs = aiMessagePairService.selectBySessionId(sessionId);
        return Result.success(aiMessagePairs);
    }
    
    @DeleteMapping("/delete")
    @Tag(name = "删除会话消息",description = "根据会话id，删除会话消息")
    public Result<String> delete(@RequestParam Long id) {
        aiMessagePairService.deleteBySessionId(id);
        return Result.success("删除成功！");
    }

    @GetMapping("/globalQuery")
    @Tag(name = "全局查询",description = "全局查询，根据关键词，查询所有会话消息")
    public Result<List<AiMessagePair>> globalQuery(@RequestParam String keyWord) throws IOException {
        return Result.success(aiMessagePairService.getFromGlobalSearch(EsConstants.INDEX_NAME_AI_MSG,keyWord));
    }
    
}
