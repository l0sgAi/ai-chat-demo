package com.losgai.ai.entity.exam;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * 考试信息表
 *
 * @TableName test
 */
@Data
public class Test implements Serializable {

    /**
     * 主键，自增ID
     */
    private Long id;
    /**
     * 考试名称
     */
    @Size(max = 255, message = "编码长度不能超过255")
    @Length(max = 255, message = "编码长度不能超过255")
    private String name;
    /**
     * 创建时间
     */
    private Date createdTime;
    /**
     * 更新时间
     * */
    private Date updatedTime;
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
    /**
     * 逻辑删除标志，0表示未删除，1表示已删除
     */
    private Integer deleted;

}
