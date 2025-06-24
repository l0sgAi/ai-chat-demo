package com.losgai.ai.controller.exam;

import cn.dev33.satoken.stp.StpUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.losgai.ai.common.Result;
import com.losgai.ai.entity.exam.QuestionBank;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.enums.SysRoleEnum;
import com.losgai.ai.service.exam.QuestionBankService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

// TODO 权限认证
@RestController
@RequiredArgsConstructor
@RequestMapping("/exam/tetsResult")
@Slf4j
public class TestResultController {


    private final QuestionBankService questionBankService;

    @PostMapping("/add")
    @Tag(name = "新增考试结果", description = "新增考试结果信息")
    public Result<String> add(@RequestBody QuestionBank questionBank) {
        ResultCodeEnum resultCodeEnum = questionBankService.add(questionBank);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("新增考试结果信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }

    @GetMapping("/query")
    @Tag(name = "查询考试结果", description = "查询考试结果信息")
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
    @Tag(name = "编辑考试结果信息", description = "编辑考试结果信息")
    public Result<String> update(@RequestBody QuestionBank questionBank) {
        ResultCodeEnum resultCodeEnum = questionBankService.update(questionBank);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("编辑考试结果信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }

    @PutMapping("/delete")
    @Tag(name = "删除考试结果信息", description = "逻辑删除考试结果信息")
    public Result<String> delete(@RequestParam Long id) {
        StpUtil.checkRole(SysRoleEnum.ADMIN.getMessage());
        ResultCodeEnum resultCodeEnum = questionBankService.delete(id);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("删除考试结果信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }

}
