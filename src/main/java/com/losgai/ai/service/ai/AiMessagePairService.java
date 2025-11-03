package com.losgai.ai.service.ai;


import com.losgai.ai.common.sys.CursorPageInfo;
import com.losgai.ai.entity.ai.AiMessagePair;

import java.io.IOException;
import java.util.List;

public interface AiMessagePairService {

    void addMessage(AiMessagePair aiMessage);

    void deleteBySessionId(Long id);

    boolean insertAiMessagePairDocBatch(String indexName,List<AiMessagePair> aiMessagePairs) throws IOException;

    boolean insertAiMessagePairDoc(String indexName,AiMessagePair aiMessagePair) throws IOException;

    List<AiMessagePair> getFromGlobalSearch(String indexName,String query) throws IOException;

    List<AiMessagePair> selectBySessionIdInitial(Long sessionId);

    CursorPageInfo<AiMessagePair> selectBySessionIdPage(Long sessionId, Long lastId, int pageSize);

    boolean delAiMessagePairDoc(String indexNameAiMsg, Long sessionId) throws IOException;
}
