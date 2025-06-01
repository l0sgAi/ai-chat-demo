package com.losgai.ai.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 一轮问答记录表
 * &#064;TableName  ai_message_pair
 */
@Data
public class AiMessagePair implements Serializable {

    /**
     * 主键，自增ID
     */
    private Long id;
    /**
     * 会话ID
     */
    private Long sessionId;
    /**
     * SSE会话ID
     */
    private String sseSessionId;
    /**
     * 用户提问内容
     */
    private String userContent;
    /**
     * AI回复内容
     */
    private String aiContent;
    /**
     * 使用模型id
     */
    private Integer modelUsed;
    /**
     * 状态：0-生成中 1-完成 2-中断
     */
    private Integer status;
    /**
     * 本轮消耗的Token
     */
    private Integer tokens;
    /**
     * 用户提问时间
     */
    private Date createTime;
    /**
     * AI回复完成时间
     */
    private Date responseTime;

}
