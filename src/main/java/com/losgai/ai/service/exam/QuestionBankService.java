package com.losgai.ai.service.exam;

import com.losgai.ai.dto.LoginDto;
import com.losgai.ai.entity.exam.QuestionBank;
import com.losgai.ai.entity.exam.User;
import com.losgai.ai.enums.ResultCodeEnum;

import java.util.List;

public interface QuestionBankService {


    ResultCodeEnum add(QuestionBank questionBank);

    ResultCodeEnum update(QuestionBank questionBank);

    ResultCodeEnum delete(Long id);

    List<QuestionBank> queryByKeyWord(String keyWord);
}
