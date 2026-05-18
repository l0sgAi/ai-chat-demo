package com.losgai.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI聊天请求参数")
public class AiChatParamDTO {

    /**
     * 对话会话id
     */
    @Schema(description = "对话会话id")
    private String chatSessionId;

    /**
     * 对话id
     */
    @Schema(description = "对话id")
    private Long conversationId;

    /**
     * 问题
     */
    @Size(min = 1, max = 5000, message = "提示词长度必须在1到5000之间")
    @Schema(description = "用户提问内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    /**
     * 模型ID
     */
    @Schema(description = "模型ID")
    private Integer modelId;

    /**
     * 模型名称
     */
    @Schema(description = "模型名称")
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
    @Schema(description = "附带的文件URL列表")
    private List<String> urlList;
}