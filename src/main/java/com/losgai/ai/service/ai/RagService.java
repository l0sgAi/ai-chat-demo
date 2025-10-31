package com.losgai.ai.service.ai;

import com.losgai.ai.common.sys.CursorPageInfo;
import com.losgai.ai.entity.ai.RagStore;

public interface RagService {

    void deleteByDocId(Long id);

    CursorPageInfo<RagStore> selectDocByPage(Long sessionId, Long lastId, int pageSize);
}
