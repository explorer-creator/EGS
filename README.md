# 丝路智联（EGS）

**「丝路」**寓意「一带一路」与对外开放，**「智联」**点明智慧连接，呼应国际物流体系与「跨境电商 + 海外仓」等政策方向。

基于 Vue + Spring Boot + xDM-F 的智慧物流与制造协同平台（仓储/分拣/路径规划、设备、物料、工序、工艺路线等），支持 Mock 模式无 8003 本地开发。

## 仓库结构

```
EGSfb/
├── front/                    # Vue 2 + Vite 主前端（生产入口）
├── proxy/                    # Spring Boot 代理层（默认 8080 → xDM-F）
├── A/                        # 模块 A：Python FastAPI 配图服务（可选）
├── business-analysis/        # 商业分析 Python 服务（可选）
├── greengeneratesysteam/     # 队友 Next.js「绿能排产」演示（iframe 嵌入）
├── docs/                     # 文档中心（含架构、部署、协作说明）
├── examples/                 # 独立示例页面（与主应用分离）
├── archive/                  # 历史碎片代码归档（勿与主工程混淆）
├── artifacts/                # 大体积 zip 等（默认不提交 Git）
├── rag_project/              # RAG / 向量实验工程
└── RAG-teammate-handoff/     # 给队友的 RAG 对接摘抄包
```

> 说明：根目录下 **`archive/`**、**`artifacts/`** 仅用于归档与大文件占位；**正式开发与构建请以 `front/`、`proxy/` 为准。**

## 快速启动

### 方案一：Mock 模式（无需 8003）

1. 在 `proxy/src/main/resources/application.yml` 中设置 `xdm.mock-mode: true`
2. 启动代理：`cd proxy && mvn spring-boot:run`
3. 启动前端：`cd front && npm install && npm run dev`
4. 访问 <http://localhost:5173>，登录 `admin` / `admin123`

### 方案二：完整环境（含 xDM-F 8003）

1. 启动 xDM-F (8003)
2. 启动代理：`cd proxy && mvn spring-boot:run`
3. 启动前端：`cd front && npm run dev`

## 评委 / 公网演示说明

- **GitHub 上的代码不会自动生成可点的演示站**；评委可 **克隆仓库后本地启动**（默认 **Mock**，**无需**本机 xDM-F 8003），见 [docs/deployment/github-push.md](docs/deployment/github-push.md)。
- **仅前端静态站（无 Java、无自己的服务器）**：`cd front && npm run build:demo`，可发布到 GitHub Pages，评委用浏览器打开 `https://<用户>.github.io/EGS/` 即可浏览（数据为内置演示快照），详见 [docs/deployment/github-pages-static-demo.md](docs/deployment/github-pages-static-demo.md)。
- 若已部署到云服务器，请在 README 或仓库简介里写明 **公网访问地址**；无 8003 时请保持 `XDM_MOCK_MODE=true`（默认），或由代理在启动时检测端口后 **自动使用模拟数据**。

## 文档

| 文档 | 说明 |
|------|------|
| [docs/README.md](docs/README.md) | **文档索引** |
| [docs/deployment/github-push.md](docs/deployment/github-push.md) | **推送到 GitHub、评委体验方式** |
| [docs/本地开发指南.md](docs/本地开发指南.md) | Mock、启动、常见问题 |
| [docs/Git协作开发指南.md](docs/Git协作开发指南.md) | 分支与 PR |
| [docs/阿里云部署指南.md](docs/阿里云部署指南.md) | 上云与 Nginx 示例 |

## 协作与规范

- [CONTRIBUTING.md](CONTRIBUTING.md) — 贡献说明  
- [LICENSE](LICENSE) — MIT  

## 技术栈

- **前端**：Vue 2 + Element UI + Vite + Axios  
- **代理**：Spring Boot 3 + RestTemplate  
- **后端**：xDM-F (8003) 或 Mock 模式  
