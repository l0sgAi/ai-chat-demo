# AI 对话服务高并发压测错误分析报告

> 分析范围：`AiChatServiceImpl.sendQuestionAsyncWithMemo()` 及其调用链
> 日期：2026-05-16

---

## 一、错误日志概览

压测中出现的 4 类错误，按因果链排列：

```
[1] Connection reset by peer                         ← 根因：客户端断连
[2] AsyncRequestNotUsableException                   ← 后果：Tomcat 标记请求不可用
[3] ai对话 流式输出报错 (RuntimeException)            ← 后果：Reactor onError 触发
[4] Operator called default onErrorDropped + NPE      ← 后果：onError 内二次异常
```

---

## 二、逐错误详细分析

### 错误 1：Connection reset by peer

**日志位置**：Tomcat ServletOutputStream

**根因**：压测客户端在 SSE 流传输过程中提前断开 TCP 连接（超时、主动关闭、网络抖动）。

**触发路径**：
```
客户端断连 → TCP RST → Tomcat ServletOutputStream.write() 抛 IOException
```

**评估**：这是高并发压测下的**预期行为**，本身不是 bug。但后续的连锁反应暴露了代码的鲁棒性问题。

---

### 错误 2：AsyncRequestNotUsableException

**日志**：
```
ServletOutputStream failed to flush: java.io.IOException: Connection reset by peer
```

**根因**：错误 1 的包装异常。Spring 检测到 Servlet 输出流失败后，将 `IOException` 封装为 `AsyncRequestNotUsableException`，标记该异步请求已不可用。

**触发路径**：
```
错误1 (IOException) → SseEmitter.send() 抛出 IOException
→ Reactor onNext 中 throw new RuntimeException(e) (L128-130 / L149-151)
→ Reactor 将异常路由到 onError 回调 (L156)
```

---

### 错误 3：ai对话 流式输出报错

**日志**：
```
ERROR c.l.a.s.ai.impl.AiChatServiceImpl - ai对话 流式输出报错
java.lang.RuntimeException: org.springframework.web.context.request.async.AsyncRequestNotUsableException
```

**根因**：Reactor `onError` 回调被触发（L156-163）。当 `onNext` 中 `finalEmitter.send()` 因客户端断连抛出 `IOException` 时，代码将其包装为 `RuntimeException` 抛出，Reactor 捕获后路由到 `onError`。

**onError 内的处理**：
```java
e -> {
    log.error("ai对话 流式输出报错", e);           // L157 — 你看到的日志
    tryUpdateMessage(aiMessagePair, ..., true);     // L160 — 更新数据库
    finalEmitter.completeWithError(e);               // L161 — 可能再次失败
    cleanupEmitters(cleaned, sessionId, conversationId); // L162
}
```

**问题**：`finalEmitter.completeWithError(e)` 在 L161 处，此时 emitter 可能已被 Tomcat 标记为不可用，此调用可能再次抛异常，形成**二次异常**。

---

### 错误 4：Operator called default onErrorDropped + NPE（最关键）

**日志**：
```
ERROR reactor.core.publisher.Operators - Operator called default onErrorDropped
java.lang.NullPointerException: Cannot invoke "Object.getClass()" because "objectToConvert" is null
  at AbstractJackson2MessageConverter.createMessage(AbstractJackson2MessageConverter.java:472)
```

**根因链**（两个独立问题叠加）：

#### 4a. NPE：RabbitMQ 消息体为 null

**触发代码**（`AiChatServiceImpl.java:173`）：
```java
aiMessageSender.sendMessage(
    "ai.exchange", "ai.message",
    aiMessagePairMapper.selectBySseSessionId(sessionId)  // ← 返回 null
);
```

**`selectBySseSessionId` 返回 null 的原因**：

`tryUpdateMessage()` 在 L234 执行的是 **UPDATE** 操作：
```java
aiMessagePairMapper.updateBySseIdSelective(message);
```
而 `aiMessagePair` 对象在 L83-85 仅设置了 `sseSessionId` 和 `sessionId`：
```java
AiMessagePair aiMessagePair = new AiMessagePair();
aiMessagePair.setSseSessionId(sessionId);
aiMessagePair.setSessionId(aiChatParamDTO.getChatSessionId());
```

如果数据库中不存在 `sse_session_id = #{sessionId}` 的记录，`UPDATE` 影响 0 行，等于**静默失败**。随后 `selectBySseSessionId` 自然返回 null。

null 被传入 `RabbitTemplate.convertAndSend()`，Jackson 反序列化时调用 `objectToConvert.getClass()` → **NPE**。

#### 4b. onErrorDropped：Reactor 丢弃异常

**触发场景**：当 `onError` 回调（L156-163）内部抛出异常时（例如 L161 的 `completeWithError` 失败，或 L173 的 NPE），Reactor 无法再次路由异常，只能调用 `Hooks.onErrorDropped`（默认行为即打印日志中的 `Operator called default onErrorDropped`）。

**完整触发链**：
```
客户端断连 → onNext 抛 RuntimeException → onError 触发
→ onError 内 completeWithError 可能失败（二次异常）
→ onComplete 也可能随后触发（Reactor 内部状态竞争）
→ onComplete 内 L173 selectBySseSessionId 返回 null → NPE
→ Reactor 丢弃该 NPE → 打印 onErrorDropped 日志
```

---

## 三、并发缺陷定位

除上述错误链外，代码中还存在以下并发隐患：

### 3.1 SseEmitterManager.addEmitter() 的 TOCTOU 竞态

**文件**：`SseEmitterManager.java:30-38`

```java
public boolean addEmitter(String sessionId, SseEmitter emitter) {
    if (emitterMap.size() < sessionLimit) {    // CHECK
        emitterMap.put(sessionId, emitter);     // ACT — 非原子！
        return true;
    }
    return false;
}
```

`ConcurrentHashMap` 的单个操作是线程安全的，但 `size() < limit` + `put()` 是**复合操作**，不是原子的。高并发下多个线程可同时通过 size 检查，导致实际连接数超过 `sessionLimit`（101）。

**影响**：过载保护失效，系统可能接受超出容量的连接。

### 3.2 Emitter 清理路径竞态（6 条清理路径）

| 路径 | 触发位置 | 移除的 key |
|------|----------|-----------|
| ① `onCompletion` 回调 | L75 | sessionId |
| ② `onTimeout` 回调 | L77-78 | sessionId |
| ③ `onError → cleanupEmitters` | L162 | sessionId + conversationId |
| ④ `onComplete → cleanupEmitters` | L168 | sessionId + conversationId |
| ⑤ heartbeat 心跳失败 | SseEmitterManager:79 | 该 entry 的 key |
| ⑥ Controller stop | AiChatController:55 | sessionId |

虽然 `cleanupEmitters` 使用 `AtomicBoolean` 防止 ③④ 重复，但 ①②⑤⑥ 与 ③④ 之间无协调。

**典型竞态场景**：
```
时间线:
T1: onComplete 触发 → cleanupEmitters 移除 sessionId
T2: onCompletion 回调触发 → removeEmitter(sessionId) — 无害但多余
T3: 同时，heartbeat 遍历到同一 entry → send 失败（emitter 已 complete）→ remove — 多余
```

更严重的是**反向竞态**：
```
T1: heartbeat 移除 emitter（因为 send 失败）
T2: onNext 检查 emitterManager.getEmitter(sessionId) == null → 设置 isInterrupted
T3: onComplete 触发 → tryUpdateMessage 标记为 STOPPED
T4: selectBySseSessionId 返回 null → NPE
```

### 3.3 心跳与流式写入的并发写冲突

**文件**：`SseEmitterManager.java:72-82`

```java
@Scheduled(fixedRate = 15000)
public void heartbeat() {
    for (Map.Entry<String, SseEmitter> entry : emitterMap.entrySet()) {
        entry.getValue().send(SseEmitter.event().comment("heartbeat"));
    }
}
```

`SseEmitter.send()` 在 Tomcat 实现中**不是线程安全的**。当心跳线程和虚拟线程中的流式写入同时调用 `send()`，可能出现：
- 输出流交错，SSE 帧格式损坏
- `IllegalStateException`（emitter 已 completed）
- 诱发 `IOException`，导致心跳误判连接断开并移除健康 emitter

### 3.4 future.join() 阻塞 Tomcat 线程

**文件**：`AiChatController.java:35`

```java
boolean accepted = future.join();  // 阻塞 Tomcat 线程
```

`sendQuestionAsyncWithMemo` 在虚拟线程中执行了同步操作（数据库查询、RAG 索引获取），`future.join()` 会阻塞 Tomcat 的平台线程直到虚拟线程完成初始化阶段。高并发下：
- Tomcat 默认线程池约 200 线程
- 每个请求阻塞等待虚拟线程初始化
- 可能导致 Tomcat 线程耗尽，新请求被拒绝

### 3.5 ChatClientFactory.streamChat() 阻塞虚拟线程

**文件**：`ChatClientFactory.java:143-170`

```java
// RAG 索引选择 — 同步阻塞调用
String indexName = chatClient.prompt()
    .system(promptProperties.getIndexFindingSystemMsg())
    .user(INDEX_FINDING_USER_MSG)
    .call()       // ← 同步 HTTP 调用！
    .content();
```

`streamChat()` 方法内部有多次**同步阻塞操作**：
- L134: `emitter.send("正在连接服务器...")` — 首次 SSE 写入
- L143-146: `ragService.getIndexes()` — HTTP 调用获取索引
- L166-170: `chatClient.prompt().call()` — **二次 AI 调用**，同步阻塞
- L154/195: 更多 SSE 状态写入

这些阻塞操作在虚拟线程中执行，虽然不会阻塞平台线程（得益于虚拟线程的自动挂起），但会占用虚拟线程的栈内存，并在高并发下积累大量同时等待 AI 响应的虚拟线程。

### 3.6 双 Emitter 设计的隐患

`sendQuestionAsyncWithMemo` 为 `sessionId` 创建 emitter，`ChatClientFactory.streamChat` 又为 `conversationId`（Long 转 String）创建 emitter。一个请求产生 **2 个 emitter**，占用双倍配额。

更严重的是，如果同一个 `conversationId` 有多个并发请求，`streamChat` 中的检查：
```java
if (sseEmitterManager.getEmitter(conversationId) == null) {
    sseEmitterManager.addEmitter(conversationId, new SseEmitter(600000L));
}
```
会复用旧 emitter，导致状态消息发给错误的 SSE 连接。

---

## 四、根因总结

| 错误 | 直接原因 | 根因 | 严重性 |
|------|---------|------|--------|
| Connection reset by peer | 客户端断连 | 压测正常现象 | 低 |
| AsyncRequestNotUsableException | Tomcat 检测断连 | 错误1 的包装 | 低 |
| 流式输出报错 | onNext 写 SSE 失败 | 缺少客户端断连的优雅处理 | 中 |
| NPE (Jackson) | selectBySseSessionId 返回 null | UPDATE 无对应记录 / 断连导致数据不一致 | **高** |
| onErrorDropped | onError 内部抛出二次异常 | 缺少 onError 内的异常隔离 | **高** |
| 超过 sessionLimit | addEmitter 非原子 | TOCTOU 竞态 | 中 |
| 心跳误杀 | 心跳与流式写并发 | SseEmitter 非线程安全 | 中 |
| Tomcat 线程耗尽 | future.join() 阻塞 | 同步等待异步初始化 | 中 |

---

## 五、关键缺陷的触发时序图

```
客户端A          Tomcat线程         虚拟线程           Reactor           心跳线程
  │                 │                 │                 │                 │
  │──POST /send──→  │                 │                 │                 │
  │                 │─future.join()─→ │                 │                 │
  │                 │  (阻塞等待)     │─创建emitter     │                 │
  │                 │                 │─streamChat()    │                 │
  │                 │                 │─subscribe()────→│                 │
  │                 │←─future.done─── │                 │─onNext(token)──→│
  │                 │                 │                 │─emitter.send()→ │
  │──断开连接──→    │                 │                 │                 │
  │                 │                 │                 │  IOException!   │
  │                 │                 │                 │─→onError()      │
  │                 │                 │                 │  tryUpdateMsg   │
  │                 │                 │                 │  completeWithError│ (可能再次失败)
  │                 │                 │                 │                 │
  │                 │                 │                 │─→onComplete()   │  ← 也可能触发
  │                 │                 │                 │  selectBySseId→ null!
  │                 │                 │                 │  sendMQ(null)→ NPE
  │                 │                 │                 │  Reactor丢弃→ onErrorDropped
  │                 │                 │                 │                 │
  │                 │                 │                 │                 │──heartbeat──→
  │                 │                 │                 │                 │  emitter.send()
  │                 │                 │                 │                 │  (emitter已completed)
  │                 │                 │                 │                 │  IOException → 移除
```

---

## 六、建议修复方向（概要，待确认后实施）

1. **onComplete 内 RabbitMQ 发送前做空值检查**，`selectBySseSessionId` 返回 null 时跳过或记录警告
2. **onError 回调内部 try-catch 隔离**，防止 `completeWithError` 的二次异常泄露到 Reactor
3. **addEmitter 使用 `ConcurrentHashMap.computeIfAbsent` 或 `putIfAbsent`** 替代 check-then-act 模式，保证原子性
4. **心跳发送时增加 emitter 状态检查**，对正在活跃写入的 emitter 跳过心跳
5. **Controller 层 `future.join()` 改为非阻塞式**，或在虚拟线程中尽早 `future.complete(true)`（当前已在 subscribe 后 complete，但前面的阻塞操作会延迟）
6. **`streamChat()` 中的 RAG 索引选择改为异步**，避免在响应流路径中进行同步 AI 调用
