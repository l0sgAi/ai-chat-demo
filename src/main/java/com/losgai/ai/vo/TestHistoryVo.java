package com.losgai.ai.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * 学生-历史考试Vo，包括考试基础信息、题目信息和考试结果
 *
 * @TableName question_bank
 */
@Data
public class TestHistoryVo implements Serializable {

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
     */
    private Long testId;
    /**
     * 考试题目内容列表-json格式字符串
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
     * 考试名称
     */
    @Size(max = 255, message = "编码长度不能超过255")
    @Length(max = 255, message = "编码长度不能超过255")
    private String name;

    /**
     * 考试开始时间
     */
    private Date startTime;

    /**
     * 考试结束时间
     */
    private Date endTime;

    /**
     * 考试持续时间-秒
     */
    private Integer durationMinutes;

}
