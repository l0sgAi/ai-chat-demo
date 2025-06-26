package com.losgai.ai.vo;

import lombok.Data;
/**
 * 用于Echart显示的VO
 * */
@Data
public class EchartDisplayVo {

    /**
     * 对应学生信息ID
     */
    private Long userId;
    /**
     * 对应学生名称
     */
    private String username;
    /**
     * 对应班级名称
     */
    private String classname;
    /**
     * 考试ID
     * */
    private Long testId;
    /**
     * 对应考试名称
     * */
    private String testName;
    /**
     * 考试用时(单位-秒)
     */
    private Integer timeUsed;
    /**
     * 最终得分
     */
    private Integer score;

}
