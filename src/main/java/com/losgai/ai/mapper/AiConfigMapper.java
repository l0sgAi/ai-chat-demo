package com.losgai.ai.mapper;

import com.losgai.ai.entity.ai.AiConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author Losgai
 * &#064;description  针对表【ai_config(AI配置信息表)】的数据库操作Mapper
 * @since 2025-06-01 12:17:12
 * &#064;Entity  generator.entity.AiConfig
 */
@Mapper
public interface AiConfigMapper {

    int deleteByPrimaryKey(Long id);

    int insert(AiConfig record);

    int insertSelective(AiConfig record);

    AiConfig selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(AiConfig record);

    int updateByPrimaryKey(AiConfig record);

    List<AiConfig> selectByKeyWord(String keyWord);

    void updateOtherIsNotDefault(Integer id);

    List<AiConfig> selectModelList();
}
