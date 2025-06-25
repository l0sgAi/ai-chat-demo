package com.losgai.ai.service.exam.impl;

import com.losgai.ai.entity.exam.Test;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.mapper.TestMapper;
import com.losgai.ai.service.exam.TestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestServiceImpl implements TestService {

    private final TestMapper testMapper;

    @Override
    @Description("新增考试信息")
    public ResultCodeEnum add(Test test) {
        // 参数校验
        if (test.getStartTime() == null || test.getEndTime() == null) {
            return ResultCodeEnum.DATA_ERROR;
        } else if (test.getStartTime().after(test.getEndTime())) {
            return ResultCodeEnum.DATA_ERROR;
        }
        // 计算EndTime-StratTime 的秒数
        long durationMinutes = test.getEndTime().getTime() - test.getStartTime().getTime();
        test.setDurationMinutes((int) durationMinutes);
        test.setCreatedTime(Date.from(Instant.now()));
        test.setUpdatedTime(Date.from(Instant.now()));
        test.setDeleted(0);
        testMapper.insert(test);
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("更新考试信息")
    public ResultCodeEnum update(Test test) {
        Date now = Date.from(Instant.now());
        // 不更新正在进行的考试
        if (now.after(test.getStartTime()) && now.before(test.getEndTime())) {
            return ResultCodeEnum.TIMING_ERROR;
        }
        // 参数校验
        if (test.getStartTime().after(test.getEndTime())) {
            return ResultCodeEnum.DATA_ERROR;
        }
        // 计算EndTime-StratTime 的秒数
        long durationMinutes = test.getEndTime().getTime() - test.getStartTime().getTime();
        test.setDurationMinutes((int) durationMinutes);
        test.setUpdatedTime(now);
        testMapper.updateByPrimaryKeySelective(test);
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("删除考试信息")
    public ResultCodeEnum delete(Long id) {
        Test test = testMapper.selectByPrimaryKey(id);
        Date now = Date.from(Instant.now());
        // 不删除正在进行的考试
        if (now.after(test.getStartTime()) && now.before(test.getEndTime())) {
            return ResultCodeEnum.TIMING_ERROR;
        }
        testMapper.deleteByPrimaryKey(id);
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("条件查询考试信息")
    public List<Test> queryByKeyWord(String keyWord, Integer status) {
        return testMapper.queryByKeyWord(keyWord, status);
    }
}
