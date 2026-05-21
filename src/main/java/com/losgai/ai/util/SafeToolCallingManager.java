package com.losgai.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全的 ToolCallingManager 包装，防止模型返回空名称工具调用时 NPE
 */
@Slf4j
public class SafeToolCallingManager implements ToolCallingManager {

    private final ToolCallingManager delegate;

    public SafeToolCallingManager() {
        this.delegate = DefaultToolCallingManager.builder().build();
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions options) {
        return delegate.resolveToolDefinitions(options);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        for (var generation : chatResponse.getResults()) {
            AssistantMessage output = generation.getOutput();
            if (output.hasToolCalls()) {
                for (var toolCall : output.getToolCalls()) {
                    if (toolCall.name() == null) {
                        log.warn("模型返回了空名称的工具调用，跳过工具执行。ToolCall: {}", toolCall);
                        return buildErrorToolResult(prompt, output);
                    }
                }
            }
        }
        return delegate.executeToolCalls(prompt, chatResponse);
    }

    private ToolExecutionResult buildErrorToolResult(Prompt prompt, AssistantMessage assistantMessage) {
        List<ToolResponseMessage.ToolResponse> errorResponses = new ArrayList<>();
        for (var toolCall : assistantMessage.getToolCalls()) {
            String toolName = toolCall.name() != null ? toolCall.name() : "unknown";
            String toolId = toolCall.id() != null ? toolCall.id() : "unknown";
            errorResponses.add(new ToolResponseMessage.ToolResponse(
                    toolId, toolName, "Error: tool call failed, please respond without using tools."));
        }

        ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
                .responses(errorResponses)
                .build();

        List<Message> history = new ArrayList<>(prompt.getInstructions());
        history.add(assistantMessage);
        history.add(toolResponseMessage);

        return ToolExecutionResult.builder()
                .conversationHistory(history)
                .build();
    }
}
