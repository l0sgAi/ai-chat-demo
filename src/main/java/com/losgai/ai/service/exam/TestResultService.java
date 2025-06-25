package com.losgai.ai.service.exam;

import com.losgai.ai.entity.exam.TestResult;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.vo.TestHistoryVo;

import java.util.List;

public interface TestResultService {


    ResultCodeEnum add(TestResult testResult);

    ResultCodeEnum update(TestResult testResult);

    ResultCodeEnum delete(Long id);

    List<TestHistoryVo> queryByKeyWord(String keyWord);

    ResultCodeEnum submit(TestResult testResult);
}
