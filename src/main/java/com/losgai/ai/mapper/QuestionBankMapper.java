package com.losgai.ai.mapper;

import com.losgai.ai.dto.StudentQuestionDto;
import com.losgai.ai.entity.exam.QuestionBank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    /**
     * 根据题目类型与难度权重，从题库生成随机题目
     *
     * @param type 题型
     * @param level 难度
     * @param limit 取出题目的数量限制
     * @return List<Test> 考试信息结果列表
     */
    List<Long> getQuestionsByTypeAndLevel(@Param("type") int type,
                                          @Param("level") int level,
                                          @Param("limit") int limit);

    /**
     * 获取所有题组ID
     *
     * @return List<QuestionIdTypeLevelDto>
     */
    List<Long> getAllQuestionGroupIds();

    List<StudentQuestionDto> selectByIds(List<Long> allQuestionGroupIds);

}
