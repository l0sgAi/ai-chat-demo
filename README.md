# AI-CHAT DEMO

### 一个AI大模型聊天机器人项目

> 对应前端的地址：https://github.com/l0sgAi/ai-chat-front

#### 项目环境需求
1. `node.js`
2. `java21+`
3. `redis`(需要自行配置)
4. `MySQL8`(需要自行配置)
5. `MySQL`数据表，建表脚本为本项目的`document`目录下的`db.sql`文件
6. 支持`OpenAI`标准的大模型`API`密钥
7. `ElasticSearch`，版本`8.15.5`

#### 项目功能介绍
_一个简单的`AI`大模型对话机器人项目。_

**使用`SpringAI`+`SpringBoot`+`MySQL`+`Redis`+`ES`开发，主要功能为：**

- 基于`MySQL`实现数据持久化管理。
- 支持简单的登录注册，`sa-token`实现，集成`Redis`储存`token`。
- 包括聊天会话和聊天问答对，支持多用户。
- 支持对话记忆和聊天上下文。
- 使用虚拟线程异步方法实现对话。
- 使用`ElasticSearch`作为向量储存实现`RAG`。
- 配置中心，可管理多个模型。(待实现)
- 支持对话的全局搜索。(待实现)
- 使用`RabbitMQ`优化异步架构。(待实现)

#### 项目启动

1. 准备项目环境。
2. 启动后端`SpringBoot`项目。
3. 启动前端项目。
4. 浏览器访问`http://localhost:5173/` (前端地址)。