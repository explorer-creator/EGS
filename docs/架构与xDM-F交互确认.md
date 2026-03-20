# 数据层、业务层、展示层均通过 xDM-F API 交互 - 确认

## 一、整体架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│  展示层 (Vue 前端)                                                       │
│  - EquipmentPage / MaterialPage / ProcedurePage / PlanPage / App.vue     │
│  - 所有数据操作通过 $axios.post/get/delete 调用 /api/dynamic/api/...     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTP (Vite proxy: /api → localhost:8080)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  代理层 (Spring Boot 8080)                                               │
│  - 纯转发，无业务逻辑                                                     │
│  - 转发到 xDM-F (baseUrl + /dynamic/api/Entity/action)                    │
│  - 携带 Cookie、X-Auth-Token、X-CSRF-TOKEN 等鉴权头                       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTP (转发到 xDM-F 8003)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  业务层 + 数据层 (xDM-F)                                                  │
│  - 提供 /dynamic/api/EquipmentManagement、MaterialManagement 等 REST API  │
│  - 业务逻辑：实体校验、生命周期、权限等                                    │
│  - 数据持久化：实体数据存储                                                │
└─────────────────────────────────────────────────────────────────────────┘
```

## 二、展示层 → xDM-F API 调用清单

| 页面/组件 | API 路径 | 用途 |
|-----------|----------|------|
| EquipmentPage | /api/dynamic/api/EquipmentManagement/list, create, update, delete | 设备 CRUD |
| MaterialPage | /api/dynamic/api/MaterialManagement/list, create, update, delete | 物料 CRUD |
| MaterialPage | /api/dynamic/api/MaterialVersionManagement/list, create | 物料版本 |
| MaterialPage | /api/dynamic/api/MaterialCategoryManagement/list, create, delete | 物料分类 |
| MaterialPage | /api/dynamic/api/MaterialBOMManagement/list, create, delete | BOM |
| ProcedurePage | /api/dynamic/api/ProcedureManagement/list, create, delete | 工序 CRUD |
| ProcedurePage | /api/dynamic/api/ProcedureEquipment/list, create, delete | 工序-设备关联 |
| ProcedurePage | /api/dynamic/api/ProcedureMaterial/list, create, delete | 工序-物料关联 |
| ProcedurePage | /api/dynamic/api/EquipmentManagement/list, MaterialManagement/list | 关联选择 |
| PlanPage | /api/dynamic/api/ProcessRoute/list, create, delete | 工艺路线 CRUD |
| PlanPage | /api/dynamic/api/ProcessRouteProcedure/list, create, delete | 工艺路线-工序关联 |
| PlanPage | /api/dynamic/api/ProcedureManagement/list | 工序选择 |
| App.vue | /api/dynamic/api/EquipmentManagement/list, MaterialManagement/list, ProcedureManagement/list, ProcessRoute/list | 全局搜索 |
| App.vue | /api/dynamic/api/User_Infor/update | 个人信息更新 |
| FeedbackBox | /api/dynamic/api/Feedback/create | 反馈提交 |
| LoginRegisterPage | /api/dynamic/api/User_Infor/create | 用户注册 |

## 三、数据流确认

- **展示层**：无直接数据库访问，所有数据均通过 `/api/dynamic/api/...` 获取
- **代理层**：无业务逻辑，仅转发请求到 xDM-F
- **业务层**：xDM-F 提供 REST API，处理业务规则
- **数据层**：xDM-F 负责数据持久化

**结论**：数据层、业务层、展示层均通过 xDM-F API 交互，符合赛题要求。

## 四、启动说明

1. **代理**：`cd D:\EGS\EGSfb\proxy; mvn spring-boot:run`（需 8080 端口空闲）
2. **前端**：`cd D:\EGS\EGSfb\front; npm run dev`（5173 端口）
3. **xDM-F**：需在 8003 端口运行，否则代理转发会失败

若 8080 被占用，可先执行：`netstat -ano | findstr :8080` 查找进程，再 `taskkill /PID <pid> /F` 结束。
