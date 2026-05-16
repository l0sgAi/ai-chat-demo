package com.losgai.ai.global;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一处理sse连接
 */
@Component
@Slf4j
@EnableScheduling
public class SseEmitterManager {
    // 支持的同时在线人数=sessionLimit-1 有一个监控sse
    @Value("${ai-chat-demo.sse.session-limit:101}")
    private int sessionLimit;

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final AtomicInteger emitterCount = new AtomicInteger(0);

    /**
     * 将对话请求加入队列（原子操作，防止TOCTOU竞态）
     */
    public boolean addEmitter(String sessionId, SseEmitter emitter) {
        if (emitterCount.incrementAndGet() > sessionLimit) {
            emitterCount.decrementAndGet();
            return false;
        }
        if (emitterMap.putIfAbsent(sessionId, emitter) != null) {
            emitterCount.decrementAndGet();
            return false;
        }
        this.notifyThreadCount();
        return true;
    }

    /**
     * 获取Map中的sse连接
     */
    public SseEmitter getEmitter(String sessionId) {
        return emitterMap.get(sessionId);
    }

    public void removeEmitter(String sessionId) {
        if (emitterMap.remove(sessionId) != null) {
            emitterCount.decrementAndGet();
            this.notifyThreadCount();
        }
    }

    public boolean isOverLoad() {
        return emitterCount.get() >= sessionLimit;
    }

    public int getEmitterCount() {
        return emitterCount.get();
    }
    
    /**
     * 获取线程监控实例
     */
    public void addThreadMonitor() {
        if (emitterMap.putIfAbsent("thread-monitor", new SseEmitter(600000L)) == null) {
            emitterCount.incrementAndGet();
        }
    }

    /**
     * 心跳保活，每15秒向所有活跃SSE连接发送注释事件
     */
    @Scheduled(fixedRate = 15000)
    public void heartbeat() {
        for (Map.Entry<String, SseEmitter> entry : emitterMap.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException e) {
                // AsyncRequestNotUsableException是IOException的子类,会被这里捕获
                log.debug("心跳发送失败, 客户端已断开, sessionId: {}", entry.getKey());
                // 原子移除，避免并发问题
                if (emitterMap.remove(entry.getKey()) != null) {
                    emitterCount.decrementAndGet();
                    log.debug("已移除断开的SSE连接, sessionId: {}, 当前连接数: {}", 
                            entry.getKey(), emitterCount.get());
                }
            } catch (Exception e) {
                // 捕获其他未知异常，防止心跳任务中断
                log.warn("心跳发送发生未知异常, sessionId: {}, 错误: {}", 
                        entry.getKey(), e.getMessage());
                if (emitterMap.remove(entry.getKey()) != null) {
                    emitterCount.decrementAndGet();
                }
            }
        }
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
            log.debug("线程监控发送失败: {}", e.getMessage());
            if (emitterMap.remove("thread-monitor") != null) {
                emitterCount.decrementAndGet();
            }
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
        emitterCount.set(0);
    }

}
