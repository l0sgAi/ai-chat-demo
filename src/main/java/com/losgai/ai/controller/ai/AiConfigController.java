package com.losgai.ai.controller.ai;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.losgai.ai.common.sys.Result;
import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.service.ai.AiConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/ai/config")
public class AiConfigController {
    
    private final AiConfigService aiConfigService;

    @GetMapping("/getModels")
    @Tag(name = "查询可用模型列表", description = "查询可用模型列表，供用户选择")
    public Result<List<AiConfig>> getModels() {
        List<AiConfig> list = aiConfigService.getModels();
        return Result.success(list);
    }

    @PostMapping("/add")
    @SaCheckRole("admin")
    @Tag(name = "新增配置",description = "新增AI配置数据")
    public Result<String> add(@RequestBody AiConfig aiConfig) {
        ResultCodeEnum res = aiConfigService.add(aiConfig);
        return Result.success(res.getMessage());
    }

    @GetMapping("/query")
    @SaCheckRole("admin")
    @Tag(name = "查询配置信息", description = "管理员根据关键字分页查询配置信息")
    public Result<List<AiConfig>> query(
            @RequestParam(required = false) String keyWord,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            // 开启分页
            PageHelper.startPage(pageNum, pageSize);

            // 执行查询
            List<AiConfig> list = aiConfigService.queryByKeyWord(keyWord);

            // 获取分页信息
            PageInfo<AiConfig> pageInfo = new PageInfo<>(list);

            // 返回分页结果
            return Result.page(list, pageInfo.getTotal());
        } finally {
            // 确保清理分页参数
            PageHelper.clearPage();
        }
    }

    @PutMapping("/update")
    @SaCheckRole("admin")
    @Tag(name = "更新配置信息",description = "更新AI配置信息")
    public Result<String> update(@RequestBody AiConfig aiConfig) {
        ResultCodeEnum res = aiConfigService.update(aiConfig);
        return Result.success(res.getMessage());
    }
    
    @DeleteMapping("/delete")
    @SaCheckRole("admin")
    @Tag(name = "删除配置信息",description = "根据配置id，删除删除配置信息")
    public Result<String> delete(@RequestParam Long id) {
        ResultCodeEnum res = aiConfigService.deleteById(id);
        return Result.success("删除成功！");
    }
    
}
