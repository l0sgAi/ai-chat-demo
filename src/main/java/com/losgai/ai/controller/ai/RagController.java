package com.losgai.ai.controller.ai;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.losgai.ai.common.sys.CursorPageInfo;
import com.losgai.ai.common.sys.Result;
import com.losgai.ai.dto.RagStoreDto;
import com.losgai.ai.entity.ai.RagStore;
import com.losgai.ai.service.ai.RagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
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
    public Result<String> add(@RequestBody @Valid RagStoreDto ragStore) {
        ragService.add(ragStore);
        return Result.success("新增文档成功！");
    }

    @SaCheckRole("admin")
    @PostMapping("/embedding")
    @Tag(name = "嵌入文档至向量数据库",description = "批量嵌入文档至向量数据库ES")
    public Result<String> embedding(@RequestBody List<Long> ids) throws IOException {
        ragService.embedding(ids);
        return Result.success("嵌入文档至向量数据库成功！");
    }

    @SaCheckRole("admin")
    @GetMapping("/getIndexes")
    @Tag(name = "获取所有向量索引",description = "获取所有向量索引列表")
    public Result<List<String>> getIndexes() throws IOException {
        List<String> list= ragService.getIndexes();
        return Result.success(list);
    }

    @SaCheckRole("admin")
    @GetMapping("/page")
    @Tag(name = "分页查询RAG文档",description = "根据排序字段，游标分页查询RAG文档")
    public Result<List<RagStore>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "2125-11-01 18:22:51") String lastUpdateTime,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        CursorPageInfo<RagStore> pageInfo = ragService.selectDocByPage(
                keyword,
                startTime,
                endTime,
                status,
                lastUpdateTime,
                pageSize);
        return Result.page(pageInfo.getList(),pageInfo.getTotal());
    }

    @SaCheckRole("admin")
    @PostMapping ("/deleteBatch")
    @Tag(name = "删除会话消息",description = "根据会话ids，删除会话消息")
    public Result<String> deleteBatch(@RequestBody List<Long> ids) {
        ragService.deleteByDocIdBatch(ids);
        return Result.success("删除成功！");
    }

    @SaCheckRole("admin")
    @DeleteMapping("/delete")
    @Tag(name = "删除会话消息",description = "根据会话id，删除会话消息")
    public Result<String> delete(@RequestParam Long id) throws IOException {
        ragService.deleteByDocId(id);
        return Result.success("删除成功！");
    }
}
