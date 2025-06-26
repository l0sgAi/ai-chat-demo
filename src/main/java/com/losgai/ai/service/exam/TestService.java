package com.losgai.ai.service.exam;

import com.losgai.ai.entity.exam.Test;
import com.losgai.ai.enums.ResultCodeEnum;

import java.util.List;

public interface TestService {


    ResultCodeEnum add(Test test);

    ResultCodeEnum update(Test test);

    ResultCodeEnum delete(Long id);

    List<Test> queryByKeyWord(String keyWord,Integer status);

    List<Test> queryByKeyWordAdmin(String keyWord, Integer status);
}
