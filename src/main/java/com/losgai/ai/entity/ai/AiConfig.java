package com.losgai.ai.entity.ai;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI配置信息表
 * &#064;TableName  ai_config
 */
@Data
public class AiConfig implements Serializable {

    /**
     * 主键，自增ID
     */
    private Integer id;
    /**
     * 显示名称
     */
    private String displayName;
    /**
     * API域名
     */
    private String apiDomain;
    /**
     * 模型名称
     */
    private String modelName;
    /**
     * 模型类型：0-大模型，1-文本向量，2-视觉模型
     */
    private Integer modelType;
    /**
     * 模型ID
     */
    private String modelId;
    /**
     * API密钥
     */
    private String apiKey;
    /**
     * 上下文最大消息数
     */
    private Integer maxContextMsgs;
    /**
     * 相似度TopP
     */
    private Double similarityTopP;
    /**
     * 随机度temperature
     */
    private Double temperature;
    /**
     * 相似度TopK
     */
    private Double similarityTopK;
    /**
     * 是否为默认模型(是0/否1)
     */
    private Integer isDefault;
    /**
     * 标签
     */
    private String caseTags;
    /**
     * 简介
     */
    private String caseBrief;
    /**
     * 备注
     */
    private String caseRemark;
    /**
     * 是否启用
     * */
    private Integer isEnabled;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;

}
