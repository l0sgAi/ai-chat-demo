# AI-CHAT DEMO

### 测试方法

**测试网址**

```bash
POST http://localhost:8585/chat/send
```

**测试请求体**

```json
{
  "question": "你是谁？",
  "modelId": 1,
  "apiKey": "your-API-key",
  "apiDomain": "https://dashscope.aliyuncs.com/compatible-mode/v1"
}
```

**参数解释**

* `question`: 问题

* `modelId`:数据库中对应配置的主键ID

* `apiKey`:你的大模型`API-KEY`

* `apiDomain`:你的API域名
