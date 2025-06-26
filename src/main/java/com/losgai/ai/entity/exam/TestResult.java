package com.losgai.ai.entity.exam;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 考试结果表
 *
 * @TableName test_result
 */
@Data
public class TestResult implements Serializable {

    /**
     * 主键，自增ID
     */
    private Long id;
    /**
     * 对应用户ID
     */
    private Long userId;
    /**
     * 考试ID
     * */
    private Long testId;
    /**
     * 考试结果报告内容-html格式
     */
    private String content;
    /**
     * 考试用时(单位-秒)
     */
    private Integer timeUsed;
    /**
     * 最终得分
     */
    private Integer score;
    /**
     * 状态0-初始 1-已经保存 2-已经提交 3-评分异常
     *
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createdTime;
    /**
     * 逻辑删除标志，0表示未删除，1表示已删除
     */
    private Integer deleted;
}
