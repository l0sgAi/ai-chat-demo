package com.losgai.ai.mapper;

import com.losgai.ai.entity.ai.AiSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Set;

/**
* @author Losgai
* &#064;description  针对表【ai_session(AI会话记录表)】的数据库操作Mapper
* @since  2025-06-01 12:17:12
 */
@Mapper
public interface AiSessionMapper {

    int deleteByPrimaryKey(Long id);

    int insert(AiSession record);

    int insertSelective(AiSession record);

    AiSession selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(AiSession record);

    int updateByPrimaryKey(AiSession record);

    /**
     * 查询用户对应的所有会话
     */
    List<AiSession> selectAllByUserId(Long userId);

    Set<Long> selectAllIdsByUserId(Long userId);

    List<AiSession> selectAllByUserIdPage(long userId,String lastMessageTime, int pageSize);

    Long countSessionByUserId(long userId);
}
