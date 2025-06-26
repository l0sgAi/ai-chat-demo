package com.losgai.ai.controller.exam;

import cn.dev33.satoken.stp.StpUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.losgai.ai.common.Result;
import com.losgai.ai.entity.exam.TestResult;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.enums.SysRoleEnum;
import com.losgai.ai.service.exam.TestResultService;
import com.losgai.ai.vo.EchartDisplayVo;
import com.losgai.ai.vo.TestHistoryVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/exam/testResult")
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
    @Tag(name = "保存考试结果信息", description = "学生端保存考试结果信息")
    public Result<String> update(@RequestBody TestResult testResult) {
        ResultCodeEnum resultCodeEnum = testResultService.update(testResult);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("编辑考试结果信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }

    @PutMapping("/submit")
    @Tag(name = "提交考试结果信息", description = "学生端提交考试结果信息，提交后不能继续编辑，返回分数")
    public Result<Integer> submit(@RequestBody TestResult testResult) throws ExecutionException, InterruptedException {
        long loginId = StpUtil.getLoginIdAsLong();
        CompletableFuture<Integer> result = testResultService.submit(testResult, loginId);
        Integer accepted = result.join();
        // 如果不成功，会同步返回结果
        if (accepted > 100) {
            return Result.error("提交失败");
        }
        return Result.success(result.get());
    }

    @PutMapping("/delete")
    @Tag(name = "删除考试结果信息", description = "逻辑删除考试结果信息")
    public Result<String> delete(@RequestParam Long id) {
        StpUtil.checkRole(SysRoleEnum.ADMIN.getMessage());
        ResultCodeEnum resultCodeEnum = testResultService.delete(id);
        if (Objects.equals(resultCodeEnum.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            return Result.success("删除考试结果信息成功");
        }
        return Result.error(resultCodeEnum.getMessage());
    }

    @GetMapping("/getDisplay")
    @Tag(name = "获取考试结果信息", description = "管理员获取考试结果统计信息")
    public Result<List<EchartDisplayVo>> getEchartDisplay() {
        StpUtil.checkRole(SysRoleEnum.ADMIN.getMessage());
        List<EchartDisplayVo> list = testResultService.selectableVo();
        return Result.success(list);
    }

}
