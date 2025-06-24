package com.losgai.ai.service.exam.impl;

import com.losgai.ai.entity.exam.QuestionBank;
import com.losgai.ai.entity.exam.TestResult;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.mapper.QuestionBankMapper;
import com.losgai.ai.mapper.TestResultMapper;
import com.losgai.ai.service.exam.QuestionBankService;
import com.losgai.ai.service.exam.TestResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestResultServiceImpl implements TestResultService {

    private final TestResultMapper testResultMapper;

    @Override
    @Description("新增试题信息")
    public ResultCodeEnum add(TestResult testResult) {
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("更新试题信息")
    public ResultCodeEnum update(TestResult testResult) {
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("删除试题信息")
    public ResultCodeEnum delete(Long id) {
        testResultMapper.deleteByPrimaryKey(id);
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("查询试题信息")
    public List<TestResult> queryByKeyWord(String keyWord) {
        return testResultMapper.queryByKeyWord(keyWord);
    }
}
