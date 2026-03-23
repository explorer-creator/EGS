# 系统架构说明

## 一、架构先进性：灵活可配置与可扩展性

本系统采用**前后端分离 + 配置驱动**的架构，具备良好的灵活性与可扩展性，满足多环境部署与多租户扩展需求。

---

## 二、后端架构

### 2.1 配置化设计

- **application.yml**：集中管理服务端口、xDM-F 连接、CORS 等配置
- **环境变量覆盖**：支持通过环境变量动态覆盖，无需修改代码即可适配不同部署环境

| 配置项 | 环境变量 | 说明 |
|--------|----------|------|
| xDM 服务地址 | `XDM_BASE_URL` | xDM-F 后端地址 |
| 应用 ID | `XDM_APPLICATION_ID` | xDM-F 鉴权应用标识 |
| 租户 ID | `XDM_TENANT_ID` | 多租户场景 |
| CORS 允许源 | `CORS_ORIGINS` | 支持逗号分隔多前端地址 |

### 2.2 可扩展的代理层

- **BaseXdmProxyController**：抽象基类，统一鉴权头构建、请求转发、404 容错
- **XdmProxyProperties**：`@ConfigurationProperties` 注入配置，新增 xDM 参数只需在 `application.yml` 扩展
- **Controller 扩展**：新增业务实体时，继承 `BaseXdmProxyController` 并注入 `XdmProxyProperties` 即可，无需重复编写鉴权逻辑

### 2.3 CORS 可配置

- **CorsProperties**：从 `application.yml` 的 `cors.*` 读取
- 支持多前端地址（开发、测试、生产），部署时通过 `CORS_ORIGINS` 环境变量配置

---

## 三、前端架构

### 3.1 统一配置模块

- **src/config.js**：集中导出 `tenantId`、`apiBase`、`applicationId`
- **环境变量**：Vite 支持 `VITE_TENANT_ID`、`VITE_API_BASE`、`VITE_APPLICATION_ID` 覆盖
- **.env.development / .env.production**：按环境区分配置

### 3.2 可扩展的页面结构

- 各业务页面（设备、物料、工序、工艺路线）通过 `import { config } from '../config'` 获取租户等配置
- 新增业务模块时，复用 `config` 与现有工具（`exportCsv`、`parseCsv`）即可快速扩展

---

## 四、扩展点总结

| 扩展场景 | 实现方式 |
|----------|----------|
| 更换 xDM-F 地址 | 修改 `application.yml` 或设置 `XDM_BASE_URL` |
| 多租户部署 | 配置 `XDM_TENANT_ID`，前端 `VITE_TENANT_ID` |
| 新增前端部署地址 | 配置 `CORS_ORIGINS` |
| 新增业务实体代理 | 继承 `BaseXdmProxyController`，注入 `XdmProxyProperties` |
| 新增前端业务模块 | 引入 `config`、复用导出/导入工具 |

---

## 五、技术栈

- **后端**：Spring Boot 3.x、RestTemplate、`@ConfigurationProperties`
- **前端**：Vue 2、Element UI、Vite、Axios
- **数据建模**：xDM-F 动态实体（Equipment、Material01、WorkingProcedure、ProcessRoute 等）
