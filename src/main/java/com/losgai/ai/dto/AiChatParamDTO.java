package com.losgai.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiChatParamDTO {

    /**
     * 对话会话id
     */
    private Long chatSessionId;

    /**
     * 对话id
     */
    private Long conversationId;

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

//    /**
//     * 是否使用RAG
//     */
//    private Boolean useRag;
//
//    /**
//     * 是否使用MCP联网
//     */
//    private Boolean useSearch;

    /**
     * 附带的文件url列表
     * */
    private List<String> urlList;
}