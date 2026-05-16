package com.losgai.ai.common.sys;// GlobalExceptionHandler.java

import com.fasterxml.jackson.core.JsonParseException;
import com.losgai.ai.enums.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    /**
     * 处理JSON解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<String> handleJsonParseError(HttpMessageNotReadableException e) {
        String errorMessage = "请求数据格式错误";
        
        // 提取更详细的错误信息
        Throwable cause = e.getCause();
        if (cause instanceof JsonParseException) {
            JsonParseException jsonException = (JsonParseException) cause;
            errorMessage = String.format("JSON格式错误: %s", jsonException.getOriginalMessage());
            log.error("[JSON解析失败] 原始错误: {}", jsonException.getOriginalMessage(), jsonException);
        } else {
            log.error("[请求体解析失败] 请检查JSON格式是否正确", e);
        }
        
        return Result.error(errorMessage);
    }

    /**
     * 处理参数验证异常(@Valid/@Validated)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数验证失败");
        
        log.warn("[参数验证失败] {}", errorMessage);
        return Result.error(errorMessage);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<String> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数绑定失败");
        
        log.warn("[参数绑定失败] {}", errorMessage);
        return Result.error(errorMessage);
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