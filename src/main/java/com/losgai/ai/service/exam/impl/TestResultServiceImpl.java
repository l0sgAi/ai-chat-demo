package com.losgai.ai.service.exam.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.losgai.ai.dto.StudentQuestionDto;
import com.losgai.ai.entity.exam.Test;
import com.losgai.ai.entity.exam.TestResult;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.mapper.TestMapper;
import com.losgai.ai.mapper.TestResultMapper;
import com.losgai.ai.service.ai.AiChatService;
import com.losgai.ai.service.exam.TestResultService;
import com.losgai.ai.vo.TestHistoryVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestResultServiceImpl implements TestResultService {

    private final TestResultMapper testResultMapper;

    private final TestMapper testMapper;

    private final AiChatService aiChatService;

    // 系统提示词
    private static final String sys = "You are an experienced subject matter expert and examiner. " +
            "Your core task is to rigorously and fairly evaluate the extent to" +
            " which the \"Answer\" addresses the \"Question\". " +
            "You must synthesize four dimensions: [Accuracy], [Completeness], [Relevance], " +
            "and [Coherence] to determine a final score." +
            " Your output must be a single numerical score between 0 and 10 " +
            "(The number must be integer). " +
            "You are strictly forbidden from outputting any text, " +
            "explanation, or commentary besides this number.";

    // 背景提示词
    private static final String assist = "# Example Input:\n" +
            "Question: What is Newton's First Law of Motion?\n" +
            "Answer: Newton's First Law states that an object will remain at rest or in " +
            "uniform motion in a straight line unless acted upon by an external force.\n" +
            "\n" +
            "# Example Output:\n" +
            "9 (Must be integer)";

    @Override
    @Description("新增考试结果信息")
    public ResultCodeEnum add(TestResult testResult) {
        testResultMapper.insert(testResult);
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("更新考试结果信息")
    public ResultCodeEnum update(TestResult testResult) {
        // 获取对应的考试信息
        Test test = testMapper.selectByPrimaryKey(testResult.getTestId());
        Date now = Date.from(Instant.now());
        // 只有考试中的时候能提交考试结果信息
        if (now.after(test.getEndTime()) || now.before(test.getStartTime())) {
            return ResultCodeEnum.TIMING_ERROR;
        }
        // 更新用时(秒)
        long timeUsed = test.getEndTime().getTime() - now.getTime();
        timeUsed /= 1000;
        testResult.setTimeUsed((int) timeUsed);
        String content = testResult.getContent();
        // 获取答题结果列表
        List<StudentQuestionDto> studentQuestionDto = JSON.parseArray(content, StudentQuestionDto.class);
        // 通过答题结果计算分数
        int score = 0;
        for (StudentQuestionDto item : studentQuestionDto) {
            if (item.getType() == 0 &&
                    Objects.equals(item.getAnswerOption(), item.getStuAnswerOption())) {
                // 选择题，3分
                score += 3;
            } else if (item.getType() == 1 &&
                    Objects.equals(item.getAnswerOption(), item.getStuAnswerOption())) {
                // 判断题，2分
                score += 2;
            }
        }
        // 暂时保存选择与判断题，最后提交再使用AI给简答题打分
        testResult.setScore(score);
        // 执行更新(结果保存)
        testResultMapper.updateByPrimaryKeySelective(testResult);
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("删除考试结果信息")
    public ResultCodeEnum delete(Long id) {
        testResultMapper.deleteByPrimaryKey(id);
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Description("查询考试结果信息")
    public List<TestHistoryVo> queryByKeyWord(String keyWord) {
        long loginId = StpUtil.getLoginIdAsLong();
        List<TestHistoryVo> testHistoryVos = testResultMapper.queryByKeyWord(keyWord, loginId);
        // 过滤数据，只返回已经结束的考试结果
        testHistoryVos = testHistoryVos.stream()
                .filter(testHistoryVo ->
                        Date.from(Instant.now())
                                .after(testHistoryVo.getEndTime()))
                .toList();
        return testHistoryVos;
    }

    @Override
    public ResultCodeEnum submit(TestResult testResult) {
        // 获取对应的考试信息
        Test test = testMapper.selectByPrimaryKey(testResult.getTestId());
        Date now = Date.from(Instant.now());
        // 只有考试中的时候能提交考试结果信息
        if (now.after(test.getEndTime()) || now.before(test.getStartTime())) {
            return ResultCodeEnum.TIMING_ERROR;
        }
        // 更新用时(秒)
        long timeUsed = test.getEndTime().getTime() - now.getTime();
        timeUsed /= 1000;
        testResult.setTimeUsed((int) timeUsed);
        String content = testResult.getContent();
        // 获取答题结果列表
        List<StudentQuestionDto> studentQuestionDto = JSON.parseArray(content, StudentQuestionDto.class);
        // 通过答题结果计算分数
        int score = 0;
        for (StudentQuestionDto item : studentQuestionDto) {
            if (item.getType() == 0 &&
                    Objects.equals(item.getAnswerOption(), item.getStuAnswerOption())) {
                // 选择题，3分
                score += 3;
            } else if (item.getType() == 1 &&
                    Objects.equals(item.getAnswerOption(), item.getStuAnswerOption())) {
                // 判断题，2分
                score += 2;
            } else if (item.getType() == 2) { // 简答题，AI打分
                // 用户提示词
                String user = "# Content to Evaluate:\n" +
                        "Question: " + item.getContent() + "\n" +
                        "Answer:" + item.getStuAnswer() + "\n";
                CompletableFuture<String> stringCompletableFuture = aiChatService.simpleSendQuestion(sys, user, assist);
            }
        }
        // 暂时保存选择与判断题，最后提交再使用AI给简答题打分
        testResult.setScore(score);
        // 执行更新(结果保存)
        testResultMapper.updateByPrimaryKeySelective(testResult);
        return ResultCodeEnum.SUCCESS;
    }
}
