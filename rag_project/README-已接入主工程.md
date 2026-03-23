# rag_project 与主仓库关系说明

## 这是什么？

`rag_project` 是队友准备的 **RAG（检索增强生成）配套资料**，核心包括：

1. **`docker-compose.yml`**  
   一键拉起 **Qdrant** 向量数据库（镜像 `qdrant/qdrant`），暴露 **6333 REST** / **6334 gRPC**。  
   **当前主工程 Java 代码尚未直连 Qdrant**，检索层默认使用 **`proxy/src/main/resources/rag/init_knowledge.json`** 的 **关键词匹配**；后续可把 `RagKnowledgeService` 替换为 **Embedding + Qdrant Top-K**。

2. **`01-docs/向量库部署与使用说明.md`**  
   部署步骤与理想交互链路（前端 → `/api/rag/chat` → 检索 → 千问）。

3. **`02-proxy-java` 等**  
   早期参考副本；**以主仓库 `EGSfb/proxy` 里已合并的类为准**（`RagChatController`、`RagKnowledgeService`）。

## 主工程已接入内容

| 路径 | 说明 |
|------|------|
| `POST /api/rag/chat` | Body: `{"query":"用户问题"}`，返回 `success`、`answer`、可选 `retrievedPreview` |
| `GET /api/rag/status` | 知识条目数、模式说明 |
| `rag/init_knowledge.json` | 术语表（已修正为合法 JSON 数组） |

## 如何启动 Qdrant（可选）

在 **`EGSfb/rag_project`** 目录下：

```bash
docker compose up -d
```

浏览器可打开 **http://localhost:6333/dashboard** 查看集合与向量（接入 Java 客户端后才有数据）。

数据目录：同目录下 **`vector-data/`**（由 compose 挂载）。

---

*维护：接入 Qdrant 客户端后在此补充 `pom` 依赖与配置项。*
