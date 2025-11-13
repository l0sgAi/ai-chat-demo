package com.losgai.ai.util;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 自定义MCP工具回调提供器，添加日志功能
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class CustomMcpToolCallbackProvider extends AsyncMcpToolCallbackProvider {

    private final AsyncMcpToolCallbackProvider delegate;

    @NotNull
    @Override
    public ToolCallback[] getToolCallbacks() {
        ToolCallback[] originalCallbacks = delegate.getToolCallbacks();

        if (originalCallbacks.length == 0) {
            return originalCallbacks;
        }

        // 包装每个工具回调，添加SSE通知
        return Arrays.stream(originalCallbacks)
                .map(this::wrapNotification)
                .toArray(ToolCallback[]::new);
    }

    private ToolCallback wrapNotification(ToolCallback originalCallback) {

        return new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                ToolDefinition toolDefinition = originalCallback.getToolDefinition();
                log.info("获取工具定义: {}", toolDefinition);
                return toolDefinition;
            }

            @Override
            public ToolMetadata getToolMetadata() {
                ToolMetadata toolMetadata = originalCallback.getToolMetadata();
                log.info("MCP工具调用开始: {}", toolMetadata);
                return toolMetadata;
            }

            @Override
            public String call(String toolInput, ToolContext toolContext) {
                String call = ToolCallback.super.call(toolInput, toolContext);
                log.info("MCP工具调用结束: {}", call);
                return call;
            }

            @Override
            public String call(String functionInput) {
                String toolName = getToolDefinition().name();
                if (StrUtil.isBlank(functionInput)) {
                    return "none";
                }
                try {
                    log.info("MCP工具调用开始: {} 参数: {}", toolName, functionInput);
                    // 执行实际的工具调用
                    String result = originalCallback.call(functionInput);
                    log.info("MCP工具调用成功: {} 结果: {}", toolName, result);

                    return result;
                } catch (Exception e) {
                    log.error("MCP工具调用失败: {} 错误: {}", toolName, e.getMessage(), e);
                    throw e;
                }
            }
        };
    }
}