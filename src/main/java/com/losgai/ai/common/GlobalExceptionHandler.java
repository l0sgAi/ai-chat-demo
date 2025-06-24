package com.losgai.ai.common;// GlobalExceptionHandler.java

import com.losgai.ai.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice  // 自动为所有方法添加 @ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<String> handleNoResourceFound(NoResourceFoundException e) {
        log.error("[资源未找到] {}", e.getMessage());
        return Result.error("资源未找到：" + e.getResourcePath());
    }

    // 处理所有未被捕获的异常（兜底）
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpectedException(Exception ex) {
        Result<Void> errorResult = Result.error("系统内部错误");
        errorResult.setCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        log.error("[全局异常捕获] 未知异常", ex);
        return errorResult;
    }
}