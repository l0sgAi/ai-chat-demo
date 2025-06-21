package com.losgai.ai.entity.ai;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI会话记录表
 * &#064;TableName  ai_session
 */
@Data
public class AiSession implements Serializable {

    /**
     * 主键，自增ID
     */
    private Long id;
    /**
     * 对话主题
     */
    private String title;
    /**
     * 是否收藏
     */
    private Integer isFavorite;
    /**
     * 创建时间
     */
    private Date createdTime;
    /**
     * 最后对话时间
     */
    private Date lastMessageTime;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 使用的模型ID
     */
    private Integer modelId;
    /**
     * 标签，逗号分隔
     */
    private String tags;
    /**
     * 对话摘要
     */
    private String summary;

}
