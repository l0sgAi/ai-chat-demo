package com.losgai.ai.common;

import com.losgai.ai.enums.ErrorCode;
import lombok.Getter;

public class EmailParseException extends RuntimeException {

    // Getter 方法
    @Getter
    private final int code; // 错误码
    private final String message; // 错误信息

    public EmailParseException(int code, String message) {
        super(message); // 保证异常堆栈信息可用
        this.code = code;
        this.message = message;
    }

    public EmailParseException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }

    @Override
    public String getMessage() {
        return message;
    }
}