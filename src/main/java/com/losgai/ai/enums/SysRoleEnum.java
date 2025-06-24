package com.losgai.ai.enums;

import lombok.Getter;

@Getter // 提供获取属性值的getter方法
public enum SysRoleEnum {

    ADMIN(1, "admin"),
    STUDENT(0, "student");

    private Integer code;      // 业务状态码
    private String message;    // 响应消息

    private SysRoleEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    }
