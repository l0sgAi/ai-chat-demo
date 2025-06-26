package com.losgai.ai.mapper;

import com.losgai.ai.entity.exam.Test;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Losgai
 * @description 针对表【test(考试结果表)】的数据库操作Mapper
 * @createDate 2025-06-24 16:42:27
 * @Entity com.losgai.ai.entity.exam.Test
 */
@Mapper
public interface TestMapper {

    int deleteByPrimaryKey(Long id);

    int insert(Test record);

    int insertSelective(Test record);

    Test selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Test record);

    int updateByPrimaryKey(Test record);

    /**
     * 根据考试状态和关键字查询考试信息
     *
     * @param keyWord 关键字
     * @param status  考试状态 0-待开始 1-进行中 2-已结束
     * @return List<Test> 考试信息结果列表
     */
    List<Test> queryByKeyWord(@Param("keyWord") String keyWord,
                              @Param("status") Integer status,
                              @Param("loginId") Long loginId);

    List<Test> queryByKeyWordAdmin(@Param("keyWord") String keyWord,
                                   @Param("status") Integer status);
}
