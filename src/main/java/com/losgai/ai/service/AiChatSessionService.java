package com.losgai.ai.service;


import com.losgai.ai.entity.AiSession;

import java.util.List;

public interface AiChatSessionService {

    Long addSession(AiSession aiSession);

    List<AiSession> selectByKeyword(String keyword);

    void deleteById(Long id);
}
