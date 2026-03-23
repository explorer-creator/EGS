# 智慧运输管理系统（TMS）

## 能力

| 模块 | 说明 |
|------|------|
| **调度与路径** | `GET /api/tms-smart/schedule-optimize`：蚁群算法 **ACO** 近似求解巡回路径；支持 **货车限行** 边、**时效窗** 软惩罚、**成本权重**（¥/km、违约惩罚）。厢式车 `VAN` 可忽略限行演示。 |
| **在途监控** | `GET /api/tms-smart/in-transit`：**OBD**（油耗、急刹、水温、故障码）+ **视觉疲劳** 分数与分级预警（演示）。 |
| **ETA（统计）** | `GET /api/tms-smart/eta-predictions`：基准时长 × 路况延误因子 + 历史 MAE，输出 ETA 与 **±分钟置信带**（对照用）。 |
| **ETA（ML 部署）** | `GET /api/tms-smart/eta-ml-predict`：综合**实时路况指数**、**天气**、**司机驾驶习惯**等特征，模拟**机器学习在线推理**响应（生产可对接 **阿里云 PAI-EAS** 等 REST 推理端点），输出**分钟级** ETA 与置信带，便于下游月台与收货安排。 |

## 扩展说明

- **强化学习**：响应中 `rlNote` 提示可与仿真环境对接训练策略；当前以 ACO 为基线。
- **实时路况**：`constraints.trafficFactor` 为占位，可接入高德/百度等路况 API。
- **PAI-EAS**：`eta-ml-predict` 当前为后端模拟推理；将网关请求转发至真实 EAS Endpoint 并传入特征向量即可，前端表格结构可保持不变。

## 前端

菜单 **「智慧TMS」** 或搜索「TMS、配载、OBD、ETA」等。
