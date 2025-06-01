package com.losgai.ai.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {

    ILLEGAL_ARGUMENT(1001, "非法参数"),
    UNKNOWN_ERROR(666, "未知异常"),
    INTERNAL_SERVER_ERROR(500, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}