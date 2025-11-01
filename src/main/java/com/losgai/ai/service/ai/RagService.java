package com.losgai.ai.service.ai;

import com.losgai.ai.common.sys.CursorPageInfo;
import com.losgai.ai.dto.RagStoreDto;
import com.losgai.ai.entity.ai.RagStore;

import java.io.IOException;
import java.util.List;

public interface RagService {

    void deleteByDocId(Long id) throws IOException;

    CursorPageInfo<RagStore> selectDocByPage(String keyword,
                                             String startTime,
                                             String endTime,
                                             Integer status,
                                             String lastUpdateTime,
                                             int pageSize);

    void add(RagStoreDto ragStore);

    List<String> getIndexes() throws IOException;

    void embedding(List<Long> ids) throws IOException;
}
