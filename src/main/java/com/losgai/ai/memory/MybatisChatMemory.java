package com.losgai.ai.memory;

import cn.hutool.core.util.StrUtil;
import com.losgai.ai.entity.ai.AiMessagePair;
import com.losgai.ai.enums.AiMessageStatusEnum;
import com.losgai.ai.mapper.AiMessagePairMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class MybatisChatMemory implements ChatMemory {

    private final AiMessagePairMapper aiMessagePairMapper;

    /**
    新增对话记忆，不通过此处管理，置空
    */
    @Override
    public void add(String conversationId, List<Message> messages) {

    }

    @Override
    public List<Message> get(String conversationId) {
        // 原问答对数据
        List<AiMessagePair> pairs = aiMessagePairMapper.selectBySessionId(Long.valueOf(conversationId));
        // 对应封装的消息列表
        List<Message> messages = new ArrayList<>();

        for (AiMessagePair pair : pairs) {
            // 只处理状态正常（完成）的一对消息
            if (pair.getStatus() != null &&
                    pair.getStatus() == AiMessageStatusEnum.FINISHED.getCode()) {
                if (StrUtil.isNotBlank(pair.getUserContent())) {
                    messages.add(new UserMessage(pair.getUserContent()));
                }
                if (StrUtil.isNotBlank(pair.getAiContent())) {
                    messages.add(new AssistantMessage(pair.getAiContent()));
                }
            }
        }

        return messages;
    }

    /**
     清除对话记忆，不通过此处管理，置空
     */
    @Override
    public void clear(String conversationId) {

    }
}