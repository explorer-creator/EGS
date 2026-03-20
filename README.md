# EGS - 设备工艺管理系统

基于 Vue + Spring Boot + xDM-F 的设备、物料、工序、工艺路线管理系统，支持 Mock 模式无 8003 本地开发。

## 项目结构

```
EGS/
├── front/           # Vue 前端 (Vite)
├── proxy/           # Spring Boot 代理层 (8080)
├── business-analysis/  # 商业分析 Python 服务
└── docs/            # 说明文档
    ├── 本地开发指南.md
    ├── Git协作开发指南.md
    ├── 架构与xDM-F交互确认.md
    └── demo/        # 展示视频（将视频文件放入此目录）
```

## 快速启动

### 方案一：Mock 模式（无需 8003）

1. 在 `proxy/src/main/resources/application.yml` 中设置 `xdm.mock-mode: true`
2. 启动代理：`cd proxy && mvn spring-boot:run`
3. 启动前端：`cd front && npm install && npm run dev`
4. 访问 http://localhost:5173，登录 `admin` / `admin123`

### 方案二：完整环境（含 xDM-F 8003）

1. 启动 xDM-F (8003)
2. 启动代理：`cd proxy && mvn spring-boot:run`
3. 启动前端：`cd front && npm run dev`

## 文档

| 文档 | 说明 |
|------|------|
| [本地开发指南](docs/本地开发指南.md) | Mock 模式、启动步骤、常见问题 |
| [Git协作开发指南](docs/Git协作开发指南.md) | 团队协作、分支、PR 流程 |
| [架构与xDM-F交互确认](docs/架构与xDM-F交互确认.md) | 系统架构、API 清单 |

## 技术栈

- **前端**：Vue 2 + Element UI + Vite + Axios
- **代理**：Spring Boot 3 + RestTemplate
- **后端**：xDM-F (8003) 或 Mock 模式
