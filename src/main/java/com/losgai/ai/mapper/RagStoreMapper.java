package com.losgai.ai.mapper;


import com.losgai.ai.common.sys.CursorPageInfo;
import com.losgai.ai.dto.RagStoreDto;
import com.losgai.ai.entity.ai.RagStore;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author miesme
* @description 针对表【rag_store(RAG向量检索文档存储表)】的数据库操作Mapper
* @createDate 2025-11-01 11:15:17
* @Entity generator.domain.RagStore
*/
@Mapper
public interface RagStoreMapper {

    int deleteByPrimaryKey(Long id);

    int insert(RagStoreDto record);

    int insertSelective(RagStore record);

    RagStore selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(RagStore record);

    int updateByPrimaryKey(RagStore record);

    List<RagStore> query(String keyword,
                                   String startTime,
                                   String endTime,
                                   Integer status,
                                   String lastUpdateTime,
                                   int pageSize);

    List<RagStore> selectByIds(List<Long> ids);

    Long selectCount();
}
