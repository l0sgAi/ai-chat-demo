package com.losgai.ai.service.exam.impl;

import com.losgai.ai.entity.exam.QuestionBank;
import com.losgai.ai.entity.exam.User;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.mapper.QuestionBankMapper;
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

    @Override
    @Description("新增试题信息")
    public ResultCodeEnum add(QuestionBank questionBank) {
        questionBank.setCreatedTime(Date.from(Instant.now()));
        questionBank.setUpdatedTime(Date.from(Instant.now()));
        questionBank.setDeleted(0);
        if(questionBank.getType() == 2){
            // 简答题
            questionBank.setAnswerOption(-1);
        }else {
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
        if(questionBank.getType() == 2){
            // 简答题
            questionBank.setAnswerOption(-1);
        }else {
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
}
