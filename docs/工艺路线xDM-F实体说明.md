# 工艺路线 xDM-F 实体说明

工艺路线功能依赖 xDM-F 中的以下实体，需在设计态创建并发布后才能使用。

## 1. ProcessRoute（工艺路线）

| 属性名 | 类型 | 说明 |
|--------|------|------|
| route_code | String | 工艺编号 |
| route_name | String | 工艺名称 |
| product | String | 所属产品 |
| version | String | 版本 |
| description | String | 工艺描述 |
| operator | String | 操作人员 |
| operation_time | String/DateTime | 操作时间 |
| equipment_usage | String | 设备使用情况 |

## 2. ProcessRouteProcedure（工艺路线工序关联）

| 属性名 | 类型 | 说明 |
|--------|------|------|
| route_id | String/Reference | 工艺路线 ID（关联 ProcessRoute） |
| procedure_id | String/Reference | 工序 ID（关联 WorkingProcedure） |
| sequence_order | Integer | 顺序（1, 2, 3...） |

## 操作步骤

1. 登录 xDM-F 设计态
2. 创建实体 **ProcessRoute**，添加上述属性
3. 创建实体 **ProcessRouteProcedure**，添加上述属性
4. 发布并部署到运行态

若 xDM-F 中实体名称不同（如 工艺路线、工艺路线工序），需在代理 `EquipmentProxyController.java` 中修改对应的 `XDM_BASE_URL` 后的实体名。
