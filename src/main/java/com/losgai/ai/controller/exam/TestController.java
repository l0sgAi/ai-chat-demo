package com.losgai.ai.controller.exam;

import cn.dev33.satoken.stp.StpUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.losgai.ai.common.Result;
import com.losgai.ai.dto.StudentQuestionDto;
import com.losgai.ai.entity.exam.Test;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.enums.SysRoleEnum;
import com.losgai.ai.service.exam.QuestionBankService;
import com.losgai.ai.service.exam.TestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/exam/tests")
@Slf4j
public class TestController {


    private final TestService testService;

    private final QuestionBankService questionBankService;

    @PostMapping("/add")
    @Tag(name = "新增考试", description = "管理员新增考试信息")
    public Result<String> add(@RequestBody Test test) {
        StpUtil.checkRole(SysRoleEnum.ADMIN.getMessage());
        ResultCodeEnum resultCodeEnum = testService.add(test);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("新增考试信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }

    @GetMapping("/query")
    @Tag(name = "查询考试", description = "根据关键字和状态分页查询考试信息，其中status 0-未开始 1-进行中 2-已结束")
    public Result<List<Test>> query(
            @RequestParam(required = false) String keyWord,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);
        // 执行查询
        List<Test> list = testService.queryByKeyWord(keyWord,status);
        // 获取分页信息
        PageInfo<Test> pageInfo = new PageInfo<>(list);
        // 使用自定义分页返回方法
        return Result.page(list, pageInfo.getTotal());
    }

    @GetMapping("/queryAdmin")
    @Tag(name = "查询考试", description = "管理员根据关键字和状态分页查询考试信息，其中status 0-未开始 1-进行中 2-已结束")
    public Result<List<Test>> queryAdmin(
            @RequestParam(required = false) String keyWord,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        StpUtil.checkRole(SysRoleEnum.ADMIN.getMessage());
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);
        // 执行查询
        List<Test> list = testService.queryByKeyWordAdmin(keyWord,status);
        // 获取分页信息
        PageInfo<Test> pageInfo = new PageInfo<>(list);
        // 使用自定义分页返回方法
        return Result.page(list, pageInfo.getTotal());
    }

    @GetMapping("/getTestQuestion")
    @Tag(name = "获取考试题目", description = "根据题型和难度权重，从题库生成随机题目")
    public Result<List<StudentQuestionDto>> getTestQuestion(@RequestParam Long testId) {
        List<StudentQuestionDto> list = questionBankService.getTestQuestion(testId);
        return Result.success(list);
    }


    @PutMapping("/update")
    @Tag(name = "编辑考试信息", description = "管理员编辑考试信息")
    public Result<String> update(@RequestBody Test test) {
        StpUtil.checkRole(SysRoleEnum.ADMIN.getMessage());
        ResultCodeEnum resultCodeEnum = testService.update(test);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("编辑考试信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }

    @PutMapping("/delete")
    @Tag(name = "逻辑删除考试信息", description = "管理员逻辑删除考试信息")
    public Result<String> delete(@RequestParam Long id) {
        StpUtil.checkRole(SysRoleEnum.ADMIN.getMessage());
        ResultCodeEnum resultCodeEnum = testService.delete(id);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("删除考试信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }


}
