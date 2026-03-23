# RAG 模块集成说明（EGS 主工程）

## 是什么？

- **RAG（Retrieval-Augmented Generation）**：先从**知识库**取出与问题相关的片段，再与用户问题一起发给**大模型**，减少胡编、便于溯源。  
- 队友目录 **`EGSfb/rag_project`** 提供：**Qdrant 向量库的 Docker 模板** + 文档；主工程内已实现 **HTTP 接口** 与 **本地术语表检索**（不启动 Docker 也能演示）。

## 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/rag/chat` | JSON：`{"query":"你的问题"}` → `success`、`answer` |
| `GET` | `/api/rag/status` | 知识条数、当前模式 `keyword-rag` |

前端 **小助手**（`RobotAgent.vue`）已 **优先调用** `/api/rag/chat`，失败再回退 `/api/llm/chat`。

## 知识库文件

- `proxy/src/main/resources/rag/init_knowledge.json`  
  工业/绿色/智慧物流等 **术语与定义**；可增删条目，重启 Java 后生效。

## 向量库 Qdrant（可选）

- 配置见 **`rag_project/docker-compose.yml`**，在 `rag_project` 目录执行 `docker compose up -d`。  
- **当前 Spring 代码未写入/查询 Qdrant**，仅预留队友后续把 `RagKnowledgeService` 换成 **Embedding + 向量检索**。

## 依赖

- 与现有 **通义千问** 相同：环境变量 **`DASHSCOPE_API_KEY`** 或 `application-local.yml` 中 `qwen.api-key`。
