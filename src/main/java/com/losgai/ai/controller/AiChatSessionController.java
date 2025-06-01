package com.losgai.ai.controller;

import com.losgai.ai.common.Result;
import com.losgai.ai.entity.AiSession;
import com.losgai.ai.service.AiChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/session")
public class AiChatSessionController {

    private final AiChatSessionService aiChatSessionService;

    /**
     * 返回插入的id以适配后续操作
     */
    @PostMapping("/add")
    public Result<Long> addSession(@RequestBody AiSession aiSession) {
        Long id = aiChatSessionService.addSession(aiSession);
        return Result.success(id);
    }

    /**
     * 关键字查询，为空时全部查询
     */
    @GetMapping("/select")
    public Result<List<AiSession>> addSession(@RequestParam(required = false) String keyword) {
        List<AiSession> aiSeesionList = aiChatSessionService.selectByKeyword(keyword);
        return Result.success(aiSeesionList);
    }

    /**
     * 删除会话，返回删除的会话id
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        aiChatSessionService.deleteById(id);
        return Result.success("已删除");
    }
}
