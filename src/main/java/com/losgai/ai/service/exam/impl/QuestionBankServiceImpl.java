package com.losgai.ai.service.exam.impl;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjUtil;
import com.alibaba.fastjson2.JSON;
import com.losgai.ai.dto.StudentQuestionDto;
import com.losgai.ai.entity.exam.QuestionBank;
import com.losgai.ai.entity.exam.TestResult;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.mapper.QuestionBankMapper;
import com.losgai.ai.mapper.TestResultMapper;
import com.losgai.ai.service.exam.QuestionBankService;
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
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionBankMapper questionBankMapper;

    private final TestResultMapper testResultMapper;

    @Override
    @Description("新增试题信息")
    public ResultCodeEnum add(QuestionBank questionBank) {
        questionBank.setCreatedTime(Date.from(Instant.now()));
        questionBank.setUpdatedTime(Date.from(Instant.now()));
        questionBank.setDeleted(0);
        if (questionBank.getType() == 2) {
            // 简答题
            questionBank.setAnswerOption(-1);
        } else {
            // 选择判断题
            questionBank.setAnswer("简答题");
        }
        questionBankMapper.insert(questionBank);
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("更新试题信息")
    public ResultCodeEnum update(QuestionBank questionBank) {
        questionBank.setUpdatedTime(Date.from(Instant.now()));
        if (questionBank.getType() == 2) {
            // 简答题
            questionBank.setAnswerOption(-1);
        } else {
            // 选择判断题
            questionBank.setAnswer("简答题");
        }
        questionBankMapper.updateByPrimaryKeySelective(questionBank);
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("删除试题信息")
    public ResultCodeEnum delete(Long id) {
        questionBankMapper.deleteByPrimaryKey(id);
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("查询试题信息")
    public List<QuestionBank> queryByKeyWord(String keyWord) {
        return questionBankMapper.queryByKeyWord(keyWord);
    }

    @Override
    @Description("生成随机的考试题目列表")
    public List<StudentQuestionDto> getTestQuestion(Long testId) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 先查询是否之前有保存过的考试结果
        TestResult result = testResultMapper.getExistedQuestion(userId, testId);
        // 没有查到之前的结果，开始随机生成题目
        if (ObjUtil.isNull(result)) {
            // 返回生成的题目id
            List<Long> ids = questionBankMapper.getAllQuestionGroupIds();
            // 直接根据id返回题目列表
            List<StudentQuestionDto> list = questionBankMapper.selectByIds(ids);
            // 保存当前考试与用户的考题
            TestResult testResult = new TestResult();
            testResult.setUserId(userId);
            testResult.setTestId(testId);
            // 将题目与答案列表list转换成json字符串，使用fastjson2
            testResult.setContent(JSON.toJSONString(list));
            testResult.setCreatedTime(Date.from(Instant.now()));
            testResult.setTimeUsed(0);
            testResult.setDeleted(0);
            testResult.setScore(0);
            testResult.setStatus(0);
            testResultMapper.insert(testResult);
            return list;
        }
        String questionStr = result.getContent();
        // 解析成列表
        List<StudentQuestionDto> list = JSON.parseArray(questionStr, StudentQuestionDto.class);
        return list;
    }

    @Description("加密答案")
    private static void encryptAnswer(List<StudentQuestionDto> list) {
        for (StudentQuestionDto item : list) {
            if (item.getType() == 2) {
                item.setAnswer("");
            } else {
                // 选择判断题，加密选择题判断的答案，放到Answer字段中，对比密文即可
                item.setAnswer(SaSecureUtil.sha256(String.valueOf(item.getAnswerOption())));
            }
            item.setAnswerOption(-1);
            item.setExplanation("");
        }
    }
}
