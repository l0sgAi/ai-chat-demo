package com.losgai.mcp;

import com.losgai.mcp.service.SearchService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }

    // 自动配置将自动将工具回调注册为 MCP 工具。
    // 您可以有多个 Bean 生成 ToolCallbacks。自动配置将合并它们。
    @Bean
    public ToolCallbackProvider searchTools(SearchService searchService) {
        return MethodToolCallbackProvider.builder().toolObjects(searchService).build();
    }

}
