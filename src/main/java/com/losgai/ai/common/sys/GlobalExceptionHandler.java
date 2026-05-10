package com.losgai.ai.common.sys;// GlobalExceptionHandler.java

import com.losgai.ai.enums.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<String> handleNoResourceFound(NoResourceFoundException e) {
        log.error("[资源未找到] {}", e.getMessage());
        return Result.error("资源未找到：" + e.getResourcePath());
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncTimeout(AsyncRequestTimeoutException e, HttpServletResponse response) throws IOException {
        log.warn("[请求超时] 异步请求超时");
        if (!response.isCommitted()) {
            response.reset();
            response.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT);
        }
    }

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