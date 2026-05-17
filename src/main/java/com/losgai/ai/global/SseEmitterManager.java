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
import java.util.concurrent.atomic.AtomicLong;

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

    // 活跃度追踪：记录每个emitter最后写入时间戳
    private final Map<String, AtomicLong> lastActivityMap = new ConcurrentHashMap<>();

    // 心跳只对超过此阈值(毫秒)无活动的emitter发送探测
    private static final long HEARTBEAT_IDLE_THRESHOLD_MS = 10_000L;

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
        lastActivityMap.put(sessionId, new AtomicLong(System.currentTimeMillis()));
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
            lastActivityMap.remove(sessionId);
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
     * 更新emitter活跃时间戳，每次推流写入前调用
     */
    public void touchActivity(String sessionId) {
        AtomicLong ts = lastActivityMap.get(sessionId);
        if (ts != null) {
            ts.set(System.currentTimeMillis());
        }
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
     * 心跳保活，每15秒向空闲的SSE连接发送注释事件
     * 跳过正在活跃推流的emitter，避免并发写冲突
     */
    @Scheduled(fixedRate = 15000)
    public void heartbeat() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, SseEmitter> entry : emitterMap.entrySet()) {
            String key = entry.getKey();
            AtomicLong lastActivity = lastActivityMap.get(key);

            // 跳过正在活跃推流的emitter（10秒内有写入）
            if (lastActivity != null
                    && (now - lastActivity.get()) < HEARTBEAT_IDLE_THRESHOLD_MS) {
                continue;
            }

            try {
                entry.getValue().send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException e) {
                log.debug("心跳发送失败, 客户端已断开, sessionId: {}", key);
                if (emitterMap.remove(key) != null) {
                    emitterCount.decrementAndGet();
                    lastActivityMap.remove(key);
                    log.debug("已移除断开的SSE连接, sessionId: {}, 当前连接数: {}",
                            key, emitterCount.get());
                }
            } catch (Exception e) {
                log.warn("心跳发送发生未知异常, sessionId: {}, 错误: {}",
                        key, e.getMessage());
                if (emitterMap.remove(key) != null) {
                    emitterCount.decrementAndGet();
                    lastActivityMap.remove(key);
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
            if (sseEmitter != null) {
                sseEmitter.send(this.getEmitterCount());
            } else {
                addThreadMonitor();
                sseEmitter = emitterMap.get("thread-monitor");
                if (sseEmitter != null) {
                    sseEmitter.send(this.getEmitterCount());
                }
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
        lastActivityMap.clear();
        emitterCount.set(0);
    }

}
