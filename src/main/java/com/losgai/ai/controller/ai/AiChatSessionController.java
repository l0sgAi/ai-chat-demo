package com.losgai.ai.controller.ai;

import com.losgai.ai.common.sys.CursorPageInfo;
import com.losgai.ai.common.sys.Result;
import com.losgai.ai.entity.ai.AiSession;
import com.losgai.ai.service.ai.AiChatSessionService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @Tag(name = "添加会话",description = "添加会话，返回插入的id")
    public Result<Long> addSession(@RequestBody AiSession aiSession) {
        Long id = aiChatSessionService.addSession(aiSession);
        return Result.success(id);
    }

    /**
     * 用户会话列表查询，按照游标分页进行查询
     */
    @GetMapping("/select/page")
    @Tag(name = "查询",description = "游标分页查询")
    public Result<List<AiSession>> page(
            @RequestParam(defaultValue = "2080-07-24 14:06:17") String lastMessageTime,
            @RequestParam(defaultValue = "12") int pageSize) {
        CursorPageInfo<AiSession> pageInfo = aiChatSessionService.selectByPage(
                lastMessageTime,
                pageSize);
        return Result.page(pageInfo.getList(),pageInfo.getTotal());
    }

    /**
     * 用户会话列表查询，初始时，只加载前12条数据
     */
    @GetMapping("/select/initial")
    @Tag(name = "查询",description = "初始查询12条记录")
    public Result<List<AiSession>> initialList() {
        List<AiSession> aiSeesionList = aiChatSessionService.select();
        return Result.success(aiSeesionList);
    }

    /**
     * 删除会话，返回删除的会话id
     */
    @DeleteMapping("/delete")
    @Tag(name = "删除会话",description = "删除会话，返回删除的会话id")
    public Result<String> delete(@RequestParam Long id) {
        aiChatSessionService.deleteById(id);
        return Result.success("已删除");
    }
}
