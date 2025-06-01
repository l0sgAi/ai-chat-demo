package com.losgai.ai.common;// GlobalExceptionHandler.java

import com.losgai.ai.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 处理所有未被捕获的异常（兜底）
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpectedException(Exception ex) {
        Result<Void> errorResult = Result.error("系统内部错误");
        errorResult.setCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        // 可以选择打印日志
        log.error("[全局异常捕获] 未知异常", ex);
        return errorResult;
    }
}