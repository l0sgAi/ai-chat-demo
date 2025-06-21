package com.losgai.ai.service.ai;


import com.losgai.ai.entity.ai.AiSession;

import java.util.List;

public interface AiChatSessionService {

    Long addSession(AiSession aiSession);

    List<AiSession> selectByKeyword(String keyword);

    void deleteById(Long id);
}
