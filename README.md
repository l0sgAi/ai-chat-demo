# AI-CHAT DEMO

**English | [中文](README_CN.md)**

### An AI large language model chatbot project

> Frontend repository: https://github.com/l0sgAi/ai-chat-front

#### Requirements
1. `node.js`
2. `java21+`
3. `redis` (must be configured by yourself)
4. `MySQL8` (must be configured by yourself)
5. `MySQL` database tables — the schema script is `db.sql` in the `document` directory of this project
6. An `OpenAI`-compatible LLM `API` key
7. `ElasticSearch`, version `8.15.5`

#### Features
_A simple `AI` large language model chatbot project._

**Built with `SpringAI` + `SpringBoot` + `MySQL` + `Redis` + `ES`. Main features:**

- Data persistence based on `MySQL`.
- Simple login and registration, implemented with `sa-token`, integrating `Redis` for token storage.
- Includes chat sessions and chat Q&A pairs, with multi-user support.
- Supports conversation memory and chat context.
- Asynchronous conversations implemented with virtual threads.
- Uses `ElasticSearch` as vector storage to implement `RAG`.
- Configuration center for managing multiple models. (To be implemented)
- Global search across conversations. (To be implemented)
- Asynchronous architecture optimization using `RabbitMQ`. (To be implemented)

#### Getting Started

1. Prepare the project environment.
2. Start the backend `SpringBoot` project.
3. Start the frontend project.
4. Visit `http://localhost:5173/` in your browser (frontend address).

#### Usage Notes

**After configuring the environment, register a user account first, log in, and you can start using it.**

Please note:

- Only admin users (`roleId=1`) can access the configuration page.
- There is currently no registration method for admins. Register any account first, then change the `role` field to `1` in the database.

After that, you also need to add some LLMs in the admin console.

**1. Enter Settings**
 - Log in with the admin account
 - Click the "Settings" button in the top-right corner

**2. Add a Model**
 - Click "New Configuration" in the top-left corner of the admin console
 - Enter the corresponding parameters to add a model

**3. Add model parameters and save**
 - Enable the model you want to call

_‼️ Please note: this project follows the `OpenAI` standard, so make sure your `API KEY` supports the `OpenAI` standard._

---
### MCP Notes
> Different models vary in their tool-calling capabilities. Based on current testing, the models with the best tool-calling performance are `qwen-plus-latest` and `qwen-turbo-latest`. Some models do not support `SpringAI`-style tool calling and will throw errors — feel free to experiment.
>
> `MCP` requires the corresponding `MCP Server` to be running. I have uploaded it to the [mcp-server](https://github.com/l0sgAi/ai-chat-demo/tree/mcp-server) branch.

##### Instructions

1. Pull the `mcp-server` branch and configure your own search `API KEY`, available at: [tavily-home](ttps://app.tavily.com/home).
2. Start this project on the `mcp-server` branch.
3. Start this project on the `main` branch.

---

#### Roadmap

- [x] **(2025-07-27)** Math formula rendering support.
- [x] **(2025-07-27)** Multimodal model support — upload images and get output based on image content.
- [x] **(2025-07-27)** Full-text search across sessions.
- [x] **(2025-11-02)** Knowledge base management, accessible only by admins.
- [x] **(2025-11-02)** RAG Q&A support.
- [x] **(2025-11-11)** MCP web search support.
- [ ] Image generation support, using RabbitMQ for asynchronous processing.
