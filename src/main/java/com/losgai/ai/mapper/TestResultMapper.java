package com.losgai.ai.mapper;

import com.losgai.ai.entity.exam.TestResult;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author Losgai
* @description 针对表【test_result(考试结果表)】的数据库操作Mapper
* @createDate 2025-06-24 11:24:27
* @Entity com.losgai.ai.entity.user.TestResult
*/
@Mapper
public interface TestResultMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TestResult record);

    int insertSelective(TestResult record);

    TestResult selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TestResult record);

    int updateByPrimaryKey(TestResult record);

    List<TestResult> queryByKeyWord(String keyWord);

    TestResult getExistedQuestion(Long userId, Long testId);

}
