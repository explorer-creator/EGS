# 智能仓储管理系统（WMS）模块

## 能力说明

| 能力 | 说明 |
|------|------|
| **动态货位规划** | 以历史订单行数为周转代理指标，按周转降序与货位距出库口距离升序做 **贪心匹配**，输出推荐货位与路径指数降幅（演示算法，可替换为 ML 需求预测）。 |
| **货到人调度** | 展示 AGV、四向穿梭车、机械臂的电量、坐标与任务；统一任务队列；支持 `POST /dispatch-task` 下发演示任务。 |
| **数字孪生监控** | 五类库区拥堵%、吞吐与告警；平面热力网格 + ECharts 柱状图；数据带时间波动（演示）。 |

## API（`/api/wms-smart`）

- `GET /history-orders` — 历史订单行演示数据  
- `GET /slot-optimization` — 货位优化结果与指标  
- `POST /apply-slot-plan` — 应用推荐（内存）  
- `GET /goods-to-person` — 设备与任务队列  
- `POST /dispatch-task` — body: `taskType`, `sku`  
- `GET /digital-twin` — 孪生快照  

## 前端入口

顶部菜单 **「智能WMS」**，或搜索「智能WMS、货位、货到人、数字孪生」等。
