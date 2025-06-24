package com.losgai.ai.enums;

import lombok.Getter;

@Getter // 提供获取属性值的getter方法
public enum ResultCodeEnum {

    SUCCESS(200 , "操作成功") ,
    SERVICE_ERROR(500, "内部错误"),
    LOGIN_ERROR(201 , "用户名或者密码错误"),
    VALIDATE_CODE_ERROR(202 , "验证码错误") ,
    LOGIN_AUTH(208 , "用户未登录"),
    USER_NAME_IS_EXISTS(209 , "该学号已经存在"),
    SYSTEM_ERROR(9999 , "您的网络有问题请稍后重试"),
    DATA_ERROR(204, "数据异常"),
    TIMING_ERROR(206, "不合法状态"),
    NO_USER(205, "不是合法用户"),
    ACCOUNT_STOP( 216, "账号已停用");

    private Integer code ;      // 业务状态码
    private String message ;    // 响应消息

    private ResultCodeEnum(Integer code , String message) {
        this.code = code ;
        this.message = message ;
    }

}
