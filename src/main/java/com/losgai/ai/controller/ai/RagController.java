package com.losgai.ai.controller.ai;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.losgai.ai.common.sys.CursorPageInfo;
import com.losgai.ai.common.sys.Result;
import com.losgai.ai.dto.RagBodyDto;
import com.losgai.ai.entity.ai.AiMessagePair;
import com.losgai.ai.entity.ai.RagStore;
import com.losgai.ai.service.ai.RagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/rag")
public class RagController {

    private final RagService ragService;

    @SaCheckRole("admin")
    @PostMapping("/add")
    @Tag(name = "新增RAG文档",description = "新增RAG知识库文档数据")
    public Result<String> add(@RequestBody @Valid RagBodyDto ragBodyDto) {
        return Result.success("新增文档成功！");
    }

    @SaCheckRole("admin")
    @GetMapping("/page")
    @Tag(name = "分页查询RAG文档",description = "根据排序字段，游标分页查询RAG文档")
    public Result<List<RagStore>> page(
            @RequestParam Long sessionId,
            @RequestParam(defaultValue = "0") Long lastId,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        CursorPageInfo<RagStore> pageInfo = ragService.selectDocByPage(
                sessionId,
                lastId,
                pageSize);
        return Result.page(pageInfo.getList(),pageInfo.getTotal());
    }

    @SaCheckRole("admin")
    @DeleteMapping("/delete")
    @Tag(name = "删除会话消息",description = "根据会话id，删除会话消息")
    public Result<String> delete(@RequestParam Long id) {
        ragService.deleteByDocId(id);
        return Result.success("删除成功！");
    }
}
