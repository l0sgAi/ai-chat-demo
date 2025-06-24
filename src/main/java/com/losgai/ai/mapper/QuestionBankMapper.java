package com.losgai.ai.mapper;

import com.losgai.ai.entity.exam.QuestionBank;
import com.losgai.ai.entity.exam.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author Losgai
* @description 针对表【question_bank(试题库表)】的数据库操作Mapper
* @createDate 2025-06-24 11:24:27
* @Entity com.losgai.ai.entity.user.QuestionBank
*/
@Mapper
public interface QuestionBankMapper {

    int deleteByPrimaryKey(Long id);

    int insert(QuestionBank record);

    int insertSelective(QuestionBank record);

    QuestionBank selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(QuestionBank record);

    int updateByPrimaryKey(QuestionBank record);

    List<QuestionBank> queryByKeyWord(String keyWord);
}
