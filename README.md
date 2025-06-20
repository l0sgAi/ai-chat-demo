# AI-CHAT DEMO

### 流式对话 测试方法

**测试网址**

```bash
POST http://localhost:8575/chat/send
```

**测试请求体**

```json
{
  "question": "你是谁？",
  "modelId": 1
}
```

**参数解释**

* `question`: 问题

* `modelId`:数据库中对应配置的主键ID
