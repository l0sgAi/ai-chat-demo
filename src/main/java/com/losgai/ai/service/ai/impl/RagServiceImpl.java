package com.losgai.ai.service.ai.impl;

import com.losgai.ai.common.sys.CursorPageInfo;
import com.losgai.ai.entity.ai.RagStore;
import com.losgai.ai.service.ai.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagServiceImpl implements RagService {

    @Override
    public void deleteByDocId(Long id) {

    }

    @Override
    public CursorPageInfo<RagStore> selectDocByPage(Long sessionId, Long lastId, int pageSize) {
        return null;
    }
}
