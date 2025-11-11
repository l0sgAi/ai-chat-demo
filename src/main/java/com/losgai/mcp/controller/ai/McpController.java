package com.losgai.mcp.controller.ai;

import com.losgai.mcp.global.SseEmitterManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mcp/search")
@Slf4j
public class McpController {

    private final SseEmitterManager sseEmitterManager;

    public static final String DEFAULT_EMITTER_ID = "search-emitter";

    @PostConstruct
    public void init() {
        // 添加一个线程监控
        sseEmitterManager.addThreadMonitor();
        // 添加一个默认的 SSE 监听器
        // 注册 emitter
        SseEmitter emitter = new SseEmitter(0L); // 永不超时
        String emitterId = DEFAULT_EMITTER_ID; // 或生成唯一ID

        emitter.onCompletion(() -> {
            log.info("MCP SSE 连接完成");
            sseEmitterManager.removeEmitter(emitterId);
        });

        emitter.onTimeout(() -> {
            log.warn("MCP SSE 连接超时");
            sseEmitterManager.removeEmitter(emitterId);
        });

        emitter.onError(e -> {
            log.error("MCP SSE 连接错误", e);
            sseEmitterManager.removeEmitter(emitterId);
        });

        sseEmitterManager.addEmitter(emitterId, emitter);
    }

    // 仅用于 SSE 通道建立
    @GetMapping("/sse")
    public SseEmitter connect() {
        // 这里不处理query，因为客户端首次连接只是建立会话
        SseEmitter emitter = sseEmitterManager.getEmitter(DEFAULT_EMITTER_ID);
        if (emitter == null) {
            log.warn("MCP SSE 监听器未注册");
            sseEmitterManager.addEmitter(DEFAULT_EMITTER_ID, new SseEmitter(0L));
        }
        return sseEmitterManager.getEmitter(DEFAULT_EMITTER_ID);
    }

}