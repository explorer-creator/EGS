# 发给队友：独立「按钮 + 弹窗 → 后端 → RAG → 云 LLM」对接文件清单

你的场景：**前端耦合少**（一个按钮、弹窗输入文字）；**检索在本地/自建向量服务**，不把整库上传到云；**只在拼好 prompt 后** 用现有 **DashScope 兼容接口** 调千问。下面是需要同步给队友的 **最小代码与配置**，按优先级排序。

---

## 必发（他改后端 / 对齐协议时必看）

| 文件 | 用途 |
|------|------|
| `EGSfb/proxy/src/main/java/com/example/controller/LlmController.java` | **参考实现**：`POST /api/llm/chat` 如何用 `RestTemplate` + `QwenProperties` 调 `.../chat/completions`。你新增 **RAG 接口**（例如 `POST /api/rag/chat`）可复制同一套 **Header、Body 结构**，只在调云之前插入 **向量检索 + 拼 system/user 消息**。 |
| `EGSfb/proxy/src/main/java/com/example/config/QwenProperties.java` | 千问 **baseUrl、model、apiKey** 从配置读取的方式。 |
| `EGSfb/proxy/src/main/resources/application.yml` | `qwen:` 段（**不要发含真实 Key 的 `application-local.yml`**，只发示例可用 `application-local.yml.example`）。 |
| `EGSfb/proxy/src/main/resources/application-local.yml.example` | 本地覆盖配置模板。 |
| `EGSfb/front/vite.config.js` | 开发时 **`/api` → localhost:8080`**；队友写独立前端时也要走同源 `/api`，避免 CORS。 |
| `EGSfb/docs/API-KEYS.md` | **DASHSCOPE_API_KEY**、环境变量约定。 |

---

## 建议发（前端参考：按钮 + 弹窗怎么调 `/api`）

| 文件 | 用途 |
|------|------|
| `EGSfb/front/src/components/RobotAgent.vue` | 现成 **弹层 + 输入框 + `axios.post('/api/llm/chat', { message })`** 示例；你的模块可更简单，只保留 **POST JSON** 这一段逻辑即可。 |
| `EGSfb/front/src/main.js` | `axios` 挂到 `Vue.prototype.$axios`、请求头拦截（若队友要本地联调同一套 Cookie/Token）。 |

---

## 可选发（安全 / 入口）

| 文件 | 用途 |
|------|------|
| `EGSfb/proxy/src/main/java/com/example/config/*Security*` 或全局 Security 配置（若项目里有） | 看 **`/api/**` 是否放行**，避免新接口 401。 |
| `EGSfb/docs/向量库-RAG-队友对接说明.md` | 仓库里 **尚无向量库** 的现状与 **推荐 Sidecar/FastAPI** 分工。 |

---

## 不必整仓打包时：最小 ZIP 内容

若只发「对接包」而不是整个 Git：

```
proxy/src/main/java/com/example/controller/LlmController.java
proxy/src/main/java/com/example/config/QwenProperties.java
proxy/src/main/resources/application.yml
proxy/src/main/resources/application-local.yml.example
front/vite.config.js
front/src/components/RobotAgent.vue   （可选）
docs/API-KEYS.md
docs/向量库-RAG-队友对接说明.md
```

---

## 接口约定建议（你和队友口头对齐即可）

1. **前端**（你的独立按钮）：`POST /api/rag/...` body 例如 `{ "query": "用户输入" }`（字段名你们自定，前后一致即可）。  
2. **后端流程**：`query` → **Embedding**（可用云 embedding API 或本地模型）→ **向量库 Top-K** → **拼上下文** → **再调** `chat/completions`（与 `LlmController` 相同）。  
3. **密钥**：仍用 **`DASHSCOPE_API_KEY` / `qwen.api-key`**，**只放在 Java 或向量服务环境变量**，**不要给前端**。

---

## 一句话

> **发给队友的核心是：`LlmController`（怎么调千问）+ `QwenProperties` + `application.yml` + `vite.config.js`；前端再附 `RobotAgent.vue` 当 HTTP 示例。向量库代码让他自建仓库或新模块，通过你新增的 `/api/rag/*` 挂进同一个 Spring Boot 即可。**
