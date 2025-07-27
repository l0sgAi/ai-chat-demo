package com.losgai.ai.mapper;

import com.losgai.ai.entity.ai.AiMessagePair;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author Losgai
* &#064;description  针对表【ai_message_pair(一轮问答记录表)】的数据库操作Mapper
* @since  2025-06-01 12:17:12
* &#064;Entity generator.entity.AiMessagePair
 */
@Mapper
public interface AiMessagePairMapper {

    int deleteByPrimaryKey(Long id);

    int insert(AiMessagePair record);

    int insertSelective(AiMessagePair record);

    AiMessagePair selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(AiMessagePair record);

    int updateByPrimaryKey(AiMessagePair record);

    /** 根据sseId更新会话信息*/
    int updateBySseIdSelective(AiMessagePair record);

    List<AiMessagePair> selectBySessionId(Long sessionId);

    void deleteBySessionId(Long id);

    AiMessagePair selectBySseSessionId(String conversationId);

    Long getSessionIdBySse(String sessionId);
}
