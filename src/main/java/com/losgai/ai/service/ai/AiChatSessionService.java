package com.losgai.ai.service.ai;


import com.losgai.ai.common.sys.CursorPageInfo;
import com.losgai.ai.entity.ai.AiSession;

import java.util.List;

public interface AiChatSessionService {

    Long addSession(AiSession aiSession);

    List<AiSession> select();

    void deleteById(Long id);

    CursorPageInfo<AiSession> selectByPage(String lastMessageTime, int pageSize);
}
