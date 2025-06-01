package com.losgai.ai.global;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一处理sse连接
 */
@Component
@Slf4j
public class SseEmitterManager {
    // 支持的同时在线人数=SESSION_LIMIT-1 有一个监控sse
    private static final int SESSION_LIMIT = 101;

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 将对话请求加入队列
     */
    public boolean addEmitter(String sessionId, SseEmitter emitter) {
        if (emitterMap.size() < SESSION_LIMIT) {
            emitterMap.put(sessionId, emitter);
            // 推流当前线程数
            this.notifyThreadCount();
            return true;
        }
        return false;
    }

    /**
     * 获取Map中的sse连接
     */
    public SseEmitter getEmitter(String sessionId) {
        return emitterMap.get(sessionId);
    }

    public void removeEmitter(String sessionId) {
        emitterMap.remove(sessionId);
        // 推流当前线程数
        this.notifyThreadCount();
    }

    public boolean isOverLoad() {
        return emitterMap.size() >= SESSION_LIMIT;
    }

    public int getEmitterCount() {
        return emitterMap.size();
    }
    
    /**
     * 获取线程监控实例
     */
    public void addThreadMonitor() {
        // 添加一个线程监控
        emitterMap.put("thread-monitor", new SseEmitter(0L));
    }

    /**
     * 手动发送消息，通知当前占用的线程数
     */
    public void notifyThreadCount() {
        SseEmitter sseEmitter = emitterMap.get("thread-monitor");
        try {
            if (emitterMap.containsKey("thread-monitor")) {
                sseEmitter.send(this.getEmitterCount());
            }else {
                addThreadMonitor();
                sseEmitter = emitterMap.get("thread-monitor");
                sseEmitter.send(this.getEmitterCount());
            }
        } catch (IOException e) {
            sseEmitter.completeWithError(e);
            emitterMap.remove("thread-monitor"); // 清除失效连接
        }
    }

    /**
     * 将sse连接全部关闭
     */
    public void closeAll() {
        for (SseEmitter emitter : emitterMap.values()) {
            emitter.complete();
        }
        emitterMap.clear();
    }

}