package com.losgai.ai.dto;

import lombok.Data;

@Data
public class AiChatParamDTO {

    /**
     * 对话会话id
     */
    private Long chatSessionId;

    /**
     * 问题
     */
    private String question;

    /**
     * 模型ID
     */
    private Integer modelId;

    /**
     * 模型名称
     */
    private String modelName;
}