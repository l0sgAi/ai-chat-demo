package com.losgai.ai.controller.exam;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.losgai.ai.common.Result;
import com.losgai.ai.entity.exam.QuestionBank;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.service.exam.QuestionBankService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/exam/questionBank")
@Slf4j
public class QuestionBankController {


    private final QuestionBankService questionBankService;

    @PostMapping("/add")
    @SaCheckRole("admin")
    @Tag(name = "新增题目", description = "管理员新增题目信息")
    public Result<String> add(@RequestBody QuestionBank questionBank) {
        ResultCodeEnum resultCodeEnum = questionBankService.add(questionBank);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("新增题目信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }

    @GetMapping("/query")
    @SaCheckRole("admin")
    @Tag(name = "查询题目", description = "管理员根据关键字分页查询题目信息")
    public Result<List<QuestionBank>> query(
            @RequestParam(required = false) String keyWord,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);
        // 执行查询
        List<QuestionBank> list = questionBankService.queryByKeyWord(keyWord);
        // 获取分页信息
        PageInfo<QuestionBank> pageInfo = new PageInfo<>(list);
        // 使用自定义分页返回方法
        return Result.page(list, pageInfo.getTotal());
    }


    @PutMapping("/update")
    @SaCheckRole("admin")
    @Tag(name = "编辑题目信息", description = "管理员编辑题目信息")
    public Result<String> update(@RequestBody QuestionBank questionBank) {
        ResultCodeEnum resultCodeEnum = questionBankService.update(questionBank);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("编辑题目信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }

    @PutMapping("/delete")
    @SaCheckRole("admin")
    @Tag(name = "逻辑删除题目信息", description = "管理员逻辑删除题目信息")
    public Result<String> delete(@RequestParam Long id) {
        ResultCodeEnum resultCodeEnum = questionBankService.delete(id);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("删除题目信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }


}
