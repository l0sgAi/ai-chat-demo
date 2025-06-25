package com.losgai.ai.controller.exam;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.losgai.ai.common.Result;
import com.losgai.ai.entity.exam.TestResult;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.service.exam.TestResultService;
import com.losgai.ai.vo.TestHistoryVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/exam/tetsResult")
@Slf4j
public class TestResultController {


    private final TestResultService testResultService;


    @GetMapping("/query")
    @Tag(name = "查询考试结果", description = "查询考试结果信息")
    public Result<List<TestHistoryVo>> query(
            @RequestParam(required = false) String keyWord,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);
        // 执行查询
        List<TestHistoryVo> list = testResultService.queryByKeyWord(keyWord);
        // 获取分页信息
        PageInfo<TestHistoryVo> pageInfo = new PageInfo<>(list);
        // 使用自定义分页返回方法
        return Result.page(list, pageInfo.getTotal());
    }


    @PutMapping("/update")
    @Tag(name = "更新考试结果信息", description = "学生端更新考试结果信息")
    public Result<String> update(@RequestBody TestResult testResult) {
        ResultCodeEnum resultCodeEnum = testResultService.update(testResult);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("编辑考试结果信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }

    @PutMapping("/submit")
    @Tag(name = "提交考试结果信息", description = "学生端提交考试结果信息")
    public Result<String> submit(@RequestBody TestResult testResult) {
        ResultCodeEnum resultCodeEnum = testResultService.submit(testResult);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("编辑考试结果信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }

    @PutMapping("/delete")
    @SaCheckRole("admin")
    @Tag(name = "删除考试结果信息", description = "逻辑删除考试结果信息")
    public Result<String> delete(@RequestParam Long id) {
        ResultCodeEnum resultCodeEnum = testResultService.delete(id);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("删除考试结果信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }

}
