package com.losgai.ai.entity.exam;

import javax.validation.constraints.Size;
import javax.validation.constraints.NotNull;

import java.io.Serializable;

import java.util.Date;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
* 试题库表
* @TableName question_bank
*/
@Data
public class QuestionBank implements Serializable {

    /**
    * 主键，自增ID
    */
    private Long id;
    /**
    * 试题标题
    */
    @Size(max= 255,message="编码长度不能超过255")
    @Length(max= 255,message="编码长度不能超过255")
    private String title;
    /**
    * 试题类型，选择题0,判断题1,简答题2
    */
    @NotNull(message="[试题类型，选择题0,判断题1,简答题2]不能为空")
    private Integer type;
    /**
    * 难度等级：易0、中等1、难2
    */
    @NotNull(message="[难度等级：易0、中等1、难2]不能为空")
    private Integer level;
    /**
    * 试题内容
    */
    private String content;
    /**
    * 正确答案-文本
    */
    private String answer;
    /**
    * 正确答案-选项 abcd对应0123 正确/错误 对应 0/1
    */
    private Integer answerOption;
    /**
    * 试题解析说明
    */
    private String explanation;
    /**
    * 创建时间
    */
    private Date createdTime;
    /**
    * 最后更新时间
    */
    private Date updatedTime;
    /**
    * 逻辑删除标志，0表示未删除，1表示已删除
    */
    private Integer deleted;
}
